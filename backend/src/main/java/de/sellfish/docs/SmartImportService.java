package de.sellfish.docs;

import de.sellfish.ai.LlmService;
import de.sellfish.ai.model.ChatRequest;
import de.sellfish.ai.model.ChatResult;
import de.sellfish.common.error.ApiException;
import de.sellfish.common.json.JsonExtractor;
import de.sellfish.profile.ProfileService;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Smart document import: accepts a merged PDF (or any file) with multiple documents,
 * uses OCR (Tika) + LLM to classify and split it into individual documents, then
 * uploads each with the correct type, label and optional expiry date.
 *
 * After import, derives job-relevant preferences (titles, keywords, excluded companies)
 * from the extracted content and applies them to the user's profile.
 */
@Service
public class SmartImportService {

    private static final Logger log = LoggerFactory.getLogger(SmartImportService.class);
    private static final int MAX_FILE_SIZE = 50 * 1024 * 1024; // 50 MB

    private final DocumentService documentService;
    private final TextExtractionService textExtraction;
    private final LlmService llmService;
    private final ProfileService profileService;
    private final Tika tika;

    public SmartImportService(
            DocumentService documentService,
            TextExtractionService textExtraction,
            LlmService llmService,
            ProfileService profileService) {
        this.documentService = documentService;
        this.textExtraction = textExtraction;
        this.llmService = llmService;
        this.profileService = profileService;
        this.tika = new Tika();
        this.tika.setMaxStringLength(-1);
    }

    @Transactional
    public SmartImportResult importSmart(UUID userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw ApiException.badRequest("Empty file");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw ApiException.badRequest("File too large (max 50 MB)");
        }

        byte[] content;
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "import.pdf";
        String mime = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        try {
            content = file.getBytes();
        } catch (Exception e) {
            throw ApiException.badRequest("Could not read file: " + e.getMessage());
        }

        // 1. Extract full text via Tika (OCR for scanned PDFs)
        String fullText;
        try {
            fullText = tika.parseToString(new ByteArrayInputStream(content)).strip();
        } catch (Exception e) {
            throw ApiException.badRequest("Text extraction failed: " + e.getMessage());
        }
        if (fullText.isBlank()) {
            throw ApiException.badRequest("No text could be extracted (is the file a valid document?)");
        }

        // 2. Ask the LLM to classify and split the document into sections
        List<DetectedDoc> detected = detectDocuments(userId, fullText);
        if (detected.isEmpty()) {
            // Fallback: treat the whole file as a single CV
            detected.add(new DetectedDoc("CV", filename, null));
        }

        // 3. Upload each detected document
        List<Document> uploaded = new ArrayList<>();
        for (DetectedDoc dd : detected) {
            DocumentType type = parseType(dd.type());
            String label = dd.label() != null && !dd.label().isBlank() ? dd.label() : type.name();
            try {
                Document doc = documentService.upload(userId, type, label, mime, content);
                if (dd.expiry() != null && !dd.expiry().isBlank()) {
                    doc.setParsedStruct("{\"expiry\":\"" + dd.expiry() + "\"}");
                }
                uploaded.add(doc);
            } catch (Exception e) {
                log.warn("Smart import: failed to upload {} as {}: {}", label, type, e.getMessage());
            }
        }

        // 4. Derive preferences from the extracted content
        PreferenceSuggestion suggestions = derivePreferences(userId, fullText);

        return new SmartImportResult(uploaded, suggestions);
    }

    private List<DetectedDoc> detectDocuments(UUID userId, String text) {
        String truncated = text.length() > 12000 ? text.substring(0, 12000) : text;
        String prompt =
                """
                You are a document classifier. The user uploaded a merged file that may contain
                multiple documents (CV/résumé, certificates, employment references / Arbeitszeugnis,
                cover letter, project list). Analyze the text and identify each separate document.

                Return ONLY a valid JSON array. Each element:
                {
                  "type": "CV" | "CERTIFICATE" | "REFERENCE" | "COVER_LETTER" | "PROJECT_LIST" | "OTHER",
                  "label": "a short descriptive title, e.g. 'AWS Certified Solutions Architect' or 'Employment Reference – Acme Corp 2022'",
                  "expiry": "YYYY-MM-DD if the document has an expiry date (certifications), otherwise null"
                }

                If the text is clearly a single document, return a one-element array.
                Text:
                """
                        + truncated;

        try {
            ChatResult result = llmService.chat(
                    userId,
                    ChatRequest.of("You classify documents from merged files. Reply ONLY with valid JSON.", prompt));
            String json = JsonExtractor.extract(result.content());
            return parseDetected(json);
        } catch (Exception e) {
            log.warn("Smart import: LLM classification failed, falling back to single CV: {}", e.getMessage());
            return List.of();
        }
    }

    private List<DetectedDoc> parseDetected(String json) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(json);
            List<DetectedDoc> result = new ArrayList<>();
            if (node.isArray()) {
                for (var item : node) {
                    result.add(new DetectedDoc(
                            item.path("type").asText("CV"),
                            item.path("label").asText(null),
                            item.path("expiry").isNull()
                                    ? null
                                    : item.path("expiry").asText(null)));
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Smart import: could not parse detected docs JSON: {}", e.getMessage());
            return List.of();
        }
    }

    private DocumentType parseType(String raw) {
        if (raw == null) return DocumentType.CV;
        return switch (raw.toUpperCase().trim()) {
            case "CV", "RESUME", "RÉSUMÉ", "LEBENSLAUF" -> DocumentType.CV;
            case "CERTIFICATE", "ZERTIFIKAT" -> DocumentType.CERTIFICATE;
            case "REFERENCE", "ARBEITSZEUGNIS", "EMPLOYMENT_REFERENCE" -> DocumentType.REFERENCE;
            case "COVER_LETTER", "ANschreiben", "COVER LETTER" -> DocumentType.COVER_LETTER;
            case "PROJECT_LIST", "PROJECTS" -> DocumentType.PROJECT_LIST;
            default -> DocumentType.OTHER;
        };
    }

    private PreferenceSuggestion derivePreferences(UUID userId, String text) {
        String truncated = text.length() > 8000 ? text.substring(0, 8000) : text;
        String prompt =
                """
                Based on this document text, extract information useful for a job search:
                1. Desired job titles (roles the person is qualified for / targeting)
                2. Key skills and technologies (for keyword matching)
                3. Former employers (company names to exclude — these are ex-companies)

                Return ONLY valid JSON:
                {
                  "titles": ["Senior Backend Engineer", "Platform Engineer"],
                  "keywords": ["Kafka", "Kubernetes", "Python", "Terraform"],
                  "excludedCompanies": ["Acme Corp", "Globex Inc"]
                }

                Text:
                """
                        + truncated;

        try {
            ChatResult result = llmService.chat(
                    userId,
                    ChatRequest.of(
                            "You extract structured job-search data from documents. Reply ONLY with valid JSON.",
                            prompt));
            String json = JsonExtractor.extract(result.content());
            return parsePreferences(json);
        } catch (Exception e) {
            log.warn("Smart import: preference derivation failed: {}", e.getMessage());
            return new PreferenceSuggestion(List.of(), List.of(), List.of());
        }
    }

    private PreferenceSuggestion parsePreferences(String json) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(json);
            return new PreferenceSuggestion(
                    readArray(node, "titles"), readArray(node, "keywords"), readArray(node, "excludedCompanies"));
        } catch (Exception e) {
            return new PreferenceSuggestion(List.of(), List.of(), List.of());
        }
    }

    private List<String> readArray(com.fasterxml.jackson.databind.JsonNode node, String field) {
        List<String> result = new ArrayList<>();
        var arr = node.path(field);
        if (arr.isArray()) {
            for (var v : arr) result.add(v.asText().trim());
        }
        return result.stream().filter(s -> !s.isBlank()).toList();
    }

    public record DetectedDoc(String type, String label, String expiry) {}

    public record PreferenceSuggestion(List<String> titles, List<String> keywords, List<String> excludedCompanies) {}

    public record SmartImportResult(List<Document> uploadedDocuments, PreferenceSuggestion suggestions) {}
}
