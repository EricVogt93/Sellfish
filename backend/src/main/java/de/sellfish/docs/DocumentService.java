package de.sellfish.docs;

import de.sellfish.common.error.ApiException;
import de.sellfish.cv.CvParsingService;
import de.sellfish.storage.port.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository repository;
    private final StorageService storage;
    private final TextExtractionService textExtraction;
    private final CvParsingService cvParsingService;

    public DocumentService(DocumentRepository repository,
                           StorageService storage,
                           TextExtractionService textExtraction,
                           CvParsingService cvParsingService) {
        this.repository = repository;
        this.storage = storage;
        this.textExtraction = textExtraction;
        this.cvParsingService = cvParsingService;
    }

    @Transactional(readOnly = true)
    public List<Document> list(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Document upload(UUID userId, DocumentType type, String filename, String mime, byte[] content) {
        if (content == null || content.length == 0) {
            throw ApiException.badRequest("Leere Datei");
        }
        String key = storage.newKey(userId, type.name().toLowerCase(), filename);
        storage.store(key, content, mime);

        Document doc = new Document(userId, type, key, filename);
        doc.setMime(mime);
        doc.setSizeBytes((long) content.length);
        try {
            doc.setParsedText(textExtraction.extract(content));
        } catch (DocumentProcessingException e) {
            log.warn("Textextraktion fehlgeschlagen für {}: {}", filename, e.getMessage());
        }

        // Erstes Dokument seines Typs wird automatisch primär.
        if (repository.findByUserIdAndType(userId, type).isEmpty()) {
            doc.setPrimary(true);
        }
        Document saved = repository.save(doc);

        // Strukturierung als Best-Effort (scheitert ohne LLM-Konfiguration, blockiert Upload aber nicht).
        tryStructure(saved);
        return saved;
    }

    private void tryStructure(Document doc) {
        if (doc.getParsedText() == null || doc.getParsedText().isBlank()) {
            return;
        }
        try {
            if (doc.getType() == DocumentType.CV) {
                cvParsingService.parseCv(doc.getUserId(), doc.getId(), doc.getParsedText());
                doc.setParsedStruct("{\"parsed\":true}");
                repository.save(doc);
            } else if (doc.getType() == DocumentType.PROJECT_LIST) {
                cvParsingService.parseProjects(doc.getUserId(), doc.getParsedText());
                doc.setParsedStruct("{\"parsed\":true}");
                repository.save(doc);
            }
        } catch (RuntimeException e) {
            log.info("Automatische Strukturierung übersprungen für {}: {}", doc.getId(), e.getMessage());
        }
    }

    /**
     * Explizite (Re-)Strukturierung; Fehler werden als ApiException sichtbar.
     */
    @Transactional
    public void reparse(UUID userId, UUID id) {
        Document doc = owned(userId, id);
        if (doc.getParsedText() == null || doc.getParsedText().isBlank()) {
            throw ApiException.badRequest("Kein extrahierter Text vorhanden");
        }
        if (doc.getType() == DocumentType.CV) {
            cvParsingService.parseCv(userId, id, doc.getParsedText());
        } else if (doc.getType() == DocumentType.PROJECT_LIST) {
            cvParsingService.parseProjects(userId, doc.getParsedText());
        } else {
            throw ApiException.badRequest("Strukturierung nur für CV und Projektliste verfügbar");
        }
        doc.setParsedStruct("{\"parsed\":true}");
        repository.save(doc);
    }

    @Transactional(readOnly = true)
    public DownloadedFile download(UUID userId, UUID id) {
        Document doc = owned(userId, id);
        return new DownloadedFile(doc.getFilename(), doc.getMime(), storage.load(doc.getStorageKey()));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Document doc = owned(userId, id);
        storage.delete(doc.getStorageKey());
        repository.delete(doc);
    }

    @Transactional
    public Document setPrimary(UUID userId, UUID id) {
        Document doc = owned(userId, id);
        repository.findByUserIdAndType(userId, doc.getType())
                .forEach(d -> {
                    if (d.isPrimary()) {
                        d.setPrimary(false);
                        repository.save(d);
                    }
                });
        doc.setPrimary(true);
        return repository.save(doc);
    }

    private Document owned(UUID userId, UUID id) {
        Document doc = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Dokument nicht gefunden"));
        if (!doc.getUserId().equals(userId)) {
            throw ApiException.notFound("Dokument nicht gefunden");
        }
        return doc;
    }

    public record DownloadedFile(String filename, String mime, byte[] content) {
    }
}
