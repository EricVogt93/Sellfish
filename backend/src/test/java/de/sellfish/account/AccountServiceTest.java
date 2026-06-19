package de.sellfish.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.sellfish.ai.LlmProviderConfig;
import de.sellfish.ai.LlmProviderConfigRepository;
import de.sellfish.ai.Provider;
import de.sellfish.ai.Purpose;
import de.sellfish.common.error.ApiException;
import de.sellfish.cv.CvStructuredRepository;
import de.sellfish.cv.ProjectRepository;
import de.sellfish.docs.Document;
import de.sellfish.docs.DocumentRepository;
import de.sellfish.docs.DocumentType;
import de.sellfish.feedback.FeedbackEventRepository;
import de.sellfish.generate.GeneratedDocumentRepository;
import de.sellfish.matching.JobMatchRepository;
import de.sellfish.profile.PreferencesRepository;
import de.sellfish.profile.ProfileRepository;
import de.sellfish.storage.port.StorageService;
import de.sellfish.users.User;
import de.sellfish.users.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final ProfileRepository profileRepository = mock(ProfileRepository.class);
    private final PreferencesRepository preferencesRepository = mock(PreferencesRepository.class);
    private final CvStructuredRepository cvRepository = mock(CvStructuredRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final DocumentRepository documentRepository = mock(DocumentRepository.class);
    private final JobMatchRepository matchRepository = mock(JobMatchRepository.class);
    private final GeneratedDocumentRepository generatedRepository = mock(GeneratedDocumentRepository.class);
    private final FeedbackEventRepository feedbackRepository = mock(FeedbackEventRepository.class);
    private final LlmProviderConfigRepository llmConfigRepository = mock(LlmProviderConfigRepository.class);
    private final StorageService storage = mock(StorageService.class);

    private final UserDataReaders readers = new UserDataReaders(
            profileRepository,
            preferencesRepository,
            cvRepository,
            projectRepository,
            documentRepository,
            matchRepository,
            generatedRepository,
            feedbackRepository,
            llmConfigRepository);

    private final AccountService service = new AccountService(userRepository, readers, storage);

    private void emptyCollections(UUID userId) {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(cvRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(projectRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(documentRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(matchRepository.findByUserId(userId)).thenReturn(List.of());
        when(generatedRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(feedbackRepository.findByUserIdOrderByTsDesc(userId)).thenReturn(List.of());
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportAggregatesAndOmitsProviderSecrets() {
        UUID userId = UUID.randomUUID();
        User user = new User("eric@example.com", "hash");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        emptyCollections(userId);

        LlmProviderConfig cfg = new LlmProviderConfig(userId, Provider.OPENAI, "gpt-4o", Purpose.CHAT);
        cfg.setKeyEnc("ENCRYPTED");
        when(llmConfigRepository.findByUserId(userId)).thenReturn(List.of(cfg));

        Map<String, Object> data = service.export(userId);

        assertThat(data)
                .containsKeys(
                        "account",
                        "profile",
                        "preferences",
                        "cv",
                        "projects",
                        "documents",
                        "matches",
                        "generatedDocuments",
                        "feedback",
                        "llmProviders");
        Map<String, Object> account = (Map<String, Object>) data.get("account");
        assertThat(account.get("email")).isEqualTo("eric@example.com");

        List<Map<String, Object>> providers = (List<Map<String, Object>>) data.get("llmProviders");
        assertThat(providers).hasSize(1);
        assertThat(providers.get(0)).doesNotContainKeys("keyEnc", "keyRef", "apiKey");
        assertThat(providers.get(0).get("model")).isEqualTo("gpt-4o");
    }

    @Test
    void deleteRemovesStorageThenUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(true);
        Document doc = new Document(userId, DocumentType.CV, "users/cv/key1", "cv.pdf");
        when(documentRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(doc));
        when(generatedRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());

        service.deleteAccount(userId);

        verify(storage).delete("users/cv/key1");
        verify(userRepository).deleteById(userId);
    }

    @Test
    void deleteUnknownUserThrows() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(false);
        assertThatThrownBy(() -> service.deleteAccount(userId)).isInstanceOf(ApiException.class);
        verify(storage, org.mockito.Mockito.never()).delete(any());
    }
}
