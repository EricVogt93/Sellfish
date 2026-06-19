package de.sellfish.generate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.sellfish.ai.LlmService;
import de.sellfish.common.error.ApiException;
import de.sellfish.jobs.JobRepository;
import de.sellfish.matching.JobMatchRepository;
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
class GenerationServiceOwnershipTest {

    @Mock
    JobMatchRepository matchRepository;

    @Mock
    JobRepository jobRepository;

    @Mock
    GenerationContextBuilder contextBuilder;

    @Mock
    LlmService llmService;

    @Mock
    GeneratedDocumentRepository repository;

    @Mock
    de.sellfish.profile.ProfileRepository profileRepository;

    @InjectMocks
    GenerationService service;

    @Test
    void getThrowsWhenNotFound() {
        when(repository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void getThrowsForOtherUsersDocument() {
        GeneratedDocument others = mock(GeneratedDocument.class);
        when(others.getUserId()).thenReturn(UUID.randomUUID());
        when(repository.findById(any())).thenReturn(Optional.of(others));
        assertThatThrownBy(() -> service.get(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void updateContentSaves() {
        UUID userId = UUID.randomUUID();
        GeneratedDocument doc = mock(GeneratedDocument.class);
        when(doc.getUserId()).thenReturn(userId);
        when(repository.findById(any())).thenReturn(Optional.of(doc));
        when(repository.save(doc)).thenReturn(doc);

        service.updateContent(userId, UUID.randomUUID(), "new text");
        verify(doc).setContent("new text");
        verify(repository).save(doc);
    }

    @Test
    void deleteRemovesOwnedDocument() {
        UUID userId = UUID.randomUUID();
        GeneratedDocument doc = mock(GeneratedDocument.class);
        when(doc.getUserId()).thenReturn(userId);
        when(repository.findById(any())).thenReturn(Optional.of(doc));
        service.delete(userId, UUID.randomUUID());
        verify(repository).delete(doc);
    }

    @Test
    void listDelegatesToRepository() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        assertThat(service.list(userId)).isEmpty();
        when(repository.findByUserIdAndJobMatchIdOrderByCreatedAtDesc(eq(userId), any()))
                .thenReturn(List.of());
        assertThat(service.listForMatch(userId, UUID.randomUUID())).isEmpty();
    }
}
