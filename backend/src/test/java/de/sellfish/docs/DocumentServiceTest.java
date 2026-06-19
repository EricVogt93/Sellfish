package de.sellfish.docs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.sellfish.common.error.ApiException;
import de.sellfish.cv.CvParsingService;
import de.sellfish.storage.port.StorageService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentServiceTest {

    @Mock
    DocumentRepository repository;

    @Mock
    StorageService storage;

    @Mock
    TextExtractionService textExtraction;

    @Mock
    CvParsingService cvParsingService;

    @InjectMocks
    DocumentService service;

    @Test
    void uploadRejectsEmptyContent() {
        assertThatThrownBy(() ->
                        service.upload(UUID.randomUUID(), DocumentType.CV, "f.pdf", "application/pdf", new byte[0]))
                .isInstanceOf(ApiException.class);
        verify(storage, never()).store(any(), any(), any());
    }

    @Test
    void uploadStoresAndSetsPrimaryForFirstOfType() {
        UUID userId = UUID.randomUUID();
        when(storage.newKey(eq(userId), anyString(), anyString())).thenReturn("key");
        when(textExtraction.extract(any())).thenReturn("extracted text");
        when(repository.findByUserIdAndType(userId, DocumentType.CV)).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Document doc = service.upload(userId, DocumentType.CV, "cv.pdf", "application/pdf", "bytes".getBytes());

        verify(storage).store(eq("key"), any(), eq("application/pdf"));
        assertThat(doc.isPrimary()).isTrue();
        assertThat(doc.getParsedText()).isEqualTo("extracted text");
    }

    @Test
    void uploadStructuresCvWhenTextExtracted() {
        UUID userId = UUID.randomUUID();
        when(storage.newKey(any(), any(), any())).thenReturn("key");
        when(textExtraction.extract(any())).thenReturn("cv text");
        when(repository.findByUserIdAndType(any(), any())).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.upload(userId, DocumentType.CV, "cv.pdf", "application/pdf", "b".getBytes());
        verify(cvParsingService).parseCv(eq(userId), any(), eq("cv text"));
    }

    @Test
    void reparseRejectsUnsupportedType() {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        Document doc = mock(Document.class);
        when(doc.getUserId()).thenReturn(userId);
        when(doc.getParsedText()).thenReturn("text");
        when(doc.getType()).thenReturn(DocumentType.CERTIFICATE);
        when(repository.findById(id)).thenReturn(Optional.of(doc));
        assertThatThrownBy(() -> service.reparse(userId, id)).isInstanceOf(ApiException.class);
    }

    @Test
    void downloadReturnsStoredContent() {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        Document doc = mock(Document.class);
        when(doc.getUserId()).thenReturn(userId);
        when(doc.getFilename()).thenReturn("cv.pdf");
        when(doc.getMime()).thenReturn("application/pdf");
        when(doc.getStorageKey()).thenReturn("key");
        when(repository.findById(id)).thenReturn(Optional.of(doc));
        when(storage.load("key")).thenReturn("data".getBytes());

        DocumentService.DownloadedFile file = service.download(userId, id);
        assertThat(file.filename()).isEqualTo("cv.pdf");
        assertThat(file.content()).isEqualTo("data".getBytes());
    }

    @Test
    void deleteRemovesFromStorageAndRepo() {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        Document doc = mock(Document.class);
        when(doc.getUserId()).thenReturn(userId);
        when(doc.getStorageKey()).thenReturn("key");
        when(repository.findById(id)).thenReturn(Optional.of(doc));
        service.delete(userId, id);
        verify(storage).delete("key");
        verify(repository).delete(doc);
    }

    @Test
    void ownedThrowsForOtherUsersDocument() {
        UUID userId = UUID.randomUUID();
        Document others = mock(Document.class);
        when(others.getUserId()).thenReturn(UUID.randomUUID());
        when(repository.findById(any())).thenReturn(Optional.of(others));
        assertThatThrownBy(() -> service.download(userId, UUID.randomUUID())).isInstanceOf(ApiException.class);
    }
}
