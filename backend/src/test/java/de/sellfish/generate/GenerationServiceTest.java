package de.sellfish.generate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.sellfish.ai.LlmService;
import de.sellfish.ai.model.ChatRequest;
import de.sellfish.ai.model.ChatResult;
import de.sellfish.common.error.ApiException;
import de.sellfish.jobs.Job;
import de.sellfish.jobs.JobRepository;
import de.sellfish.matching.JobMatch;
import de.sellfish.matching.JobMatchRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GenerationServiceTest {

    private final JobMatchRepository matchRepository = mock(JobMatchRepository.class);
    private final JobRepository jobRepository = mock(JobRepository.class);
    private final GenerationContextBuilder contextBuilder = mock(GenerationContextBuilder.class);
    private final LlmService llmService = mock(LlmService.class);
    private final GeneratedDocumentRepository repository = mock(GeneratedDocumentRepository.class);
    private final de.sellfish.profile.ProfileRepository profileRepository =
            mock(de.sellfish.profile.ProfileRepository.class);

    private final GenerationService service = new GenerationService(
            matchRepository, jobRepository, contextBuilder, llmService, repository, profileRepository);

    private de.sellfish.profile.UserProfile profileWithHeadline(UUID userId) {
        de.sellfish.profile.UserProfile p = new de.sellfish.profile.UserProfile(userId);
        p.setHeadline("Backend Engineer");
        return p;
    }

    private JobMatch matchFor(UUID userId, UUID matchId, UUID jobId) {
        JobMatch m = new JobMatch(userId, jobId);
        m.setId(matchId);
        return m;
    }

    @Test
    void generatesAndStoresWithVersionOne() {
        UUID userId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        Job job = new Job("BA", "fp", "Java Dev");

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(matchFor(userId, matchId, jobId)));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profileWithHeadline(userId)));
        when(contextBuilder.build(eq(userId), eq(job))).thenReturn("CONTEXT");
        when(llmService.chat(eq(userId), any(ChatRequest.class)))
                .thenReturn(new ChatResult("Sehr geehrte Damen und Herren …", "gpt-4o", 100, 200));
        when(repository.findFirstByUserIdAndJobMatchIdAndTypeOrderByVersionDesc(
                        userId, matchId, GenerationType.COVER_LETTER))
                .thenReturn(Optional.empty());
        when(repository.save(any(GeneratedDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        GeneratedDocument doc = service.generate(userId, matchId, GenerationType.COVER_LETTER);

        assertThat(doc.getContent()).startsWith("Sehr geehrte");
        assertThat(doc.getModel()).isEqualTo("gpt-4o");
        assertThat(doc.getVersion()).isEqualTo(1);
        assertThat(doc.getPromptVersion()).isEqualTo(PromptTemplates.VERSION);
        assertThat(doc.getType()).isEqualTo(GenerationType.COVER_LETTER);
    }

    @Test
    void incrementsVersionWhenPreviousExists() {
        UUID userId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(matchFor(userId, matchId, jobId)));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(new Job("BA", "fp", "Dev")));
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profileWithHeadline(userId)));
        when(contextBuilder.build(any(), any())).thenReturn("CONTEXT");
        when(llmService.chat(any(UUID.class), any(ChatRequest.class)))
                .thenReturn(new ChatResult("text", "m", null, null));

        GeneratedDocument previous = new GeneratedDocument(userId, matchId, GenerationType.MOTIVATION);
        previous.setVersion(2);
        when(repository.findFirstByUserIdAndJobMatchIdAndTypeOrderByVersionDesc(
                        userId, matchId, GenerationType.MOTIVATION))
                .thenReturn(Optional.of(previous));
        when(repository.save(any(GeneratedDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.generate(userId, matchId, GenerationType.MOTIVATION).getVersion())
                .isEqualTo(3);
    }

    @Test
    void rejectsForeignMatch() {
        UUID userId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        when(matchRepository.findById(matchId))
                .thenReturn(Optional.of(matchFor(UUID.randomUUID(), matchId, UUID.randomUUID())));
        assertThatThrownBy(() -> service.generate(userId, matchId, GenerationType.TAILORED_CV))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsEmptyProfile() {
        UUID userId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(matchFor(userId, matchId, jobId)));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(new Job("BA", "fp", "Dev")));
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.generate(userId, matchId, GenerationType.COVER_LETTER))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("profile");
    }

    @Test
    void updateRejectsForeignDocument() {
        UUID userId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        GeneratedDocument foreign =
                new GeneratedDocument(UUID.randomUUID(), UUID.randomUUID(), GenerationType.COVER_LETTER);
        when(repository.findById(docId)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.updateContent(userId, docId, "neu")).isInstanceOf(ApiException.class);
    }
}
