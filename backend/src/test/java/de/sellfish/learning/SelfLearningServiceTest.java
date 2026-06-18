package de.sellfish.learning;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.sellfish.jobs.Job;
import de.sellfish.jobs.JobRepository;
import de.sellfish.jobs.adapter.persistence.VectorStore;
import de.sellfish.matching.FeatureScorer;
import de.sellfish.matching.JobMatch;
import de.sellfish.matching.JobMatchRepository;
import de.sellfish.matching.MatchStatus;
import de.sellfish.matching.UserRankingModel;
import de.sellfish.matching.UserRankingModelRepository;
import de.sellfish.profile.PreferencesRepository;
import de.sellfish.profile.ProfileRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SelfLearningServiceTest {

    private final JobMatchRepository matchRepository = mock(JobMatchRepository.class);
    private final JobRepository jobRepository = mock(JobRepository.class);
    private final ProfileRepository profileRepository = mock(ProfileRepository.class);
    private final PreferencesRepository preferencesRepository = mock(PreferencesRepository.class);
    private final UserRankingModelRepository rankingRepository = mock(UserRankingModelRepository.class);
    private final VectorStore vectorStore = mock(VectorStore.class);

    private final SelfLearningService service = new SelfLearningService(
            matchRepository, jobRepository, profileRepository, preferencesRepository,
            rankingRepository, vectorStore, new FeatureScorer(null), new ObjectMapper());

    private final UUID userId = UUID.randomUUID();

    private static final List<MatchStatus> POSITIVE =
            List.of(MatchStatus.SAVED, MatchStatus.APPLIED, MatchStatus.INTERVIEW, MatchStatus.OFFER);
    private static final List<MatchStatus> NEGATIVE =
            List.of(MatchStatus.DISMISSED, MatchStatus.REJECTED);

    private JobMatch buildMatch(double semantic, float[] jobEmbedding) {
        Job job = new Job("BA", "fp-" + UUID.randomUUID(), "Dev");
        UUID jobId = job.getId();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(vectorStore.similarity(userId, jobId)).thenReturn(semantic);
        when(vectorStore.getJobEmbedding(jobId)).thenReturn(jobEmbedding);
        JobMatch match = new JobMatch(userId, jobId);
        match.setId(UUID.randomUUID());
        return match;
    }

    private List<JobMatch> matches(int count, double semantic) {
        List<JobMatch> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(buildMatch(semantic, new float[]{1f, 0f}));
        }
        return list;
    }

    @Test
    void trainsWeightsAndAppliesDriftWithEnoughFeedback() {
        List<JobMatch> positives = matches(4, 0.9);
        List<JobMatch> negatives = matches(4, 0.1);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(matchRepository.findByUserIdAndStatusIn(userId, POSITIVE)).thenReturn(positives);
        when(matchRepository.findByUserIdAndStatusIn(userId, NEGATIVE)).thenReturn(negatives);
        when(rankingRepository.findFirstByUserIdOrderByVersionDesc(userId)).thenReturn(Optional.empty());
        when(vectorStore.getProfileEmbedding(userId)).thenReturn(new float[]{0f, 1f});

        SelfLearningService.RetrainResult result = service.retrain(userId);

        assertThat(result.weightsTrained()).isTrue();
        assertThat(result.positives()).isEqualTo(4);
        assertThat(result.negatives()).isEqualTo(4);
        assertThat(result.driftApplied()).isTrue();
        verify(rankingRepository).save(any(UserRankingModel.class));
    }

    @Test
    void skipsTrainingWithInsufficientFeedbackButStillDrifts() {
        List<JobMatch> positives = matches(2, 0.9);
        List<JobMatch> negatives = matches(1, 0.1);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(matchRepository.findByUserIdAndStatusIn(userId, POSITIVE)).thenReturn(positives);
        when(matchRepository.findByUserIdAndStatusIn(userId, NEGATIVE)).thenReturn(negatives);
        when(vectorStore.getProfileEmbedding(userId)).thenReturn(new float[]{0f, 1f});

        SelfLearningService.RetrainResult result = service.retrain(userId);

        assertThat(result.weightsTrained()).isFalse();
        assertThat(result.driftApplied()).isTrue();
        verify(rankingRepository, never()).save(any());
        verify(vectorStore, times(1)).upsertProfileEmbedding(eq(userId), any(), eq("drift"));
    }

    @Test
    void noDriftWhenProfileEmbeddingMissing() {
        List<JobMatch> positives = matches(1, 0.9);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(matchRepository.findByUserIdAndStatusIn(userId, POSITIVE)).thenReturn(positives);
        when(matchRepository.findByUserIdAndStatusIn(userId, NEGATIVE)).thenReturn(List.of());
        when(vectorStore.getProfileEmbedding(userId)).thenReturn(new float[0]);

        SelfLearningService.RetrainResult result = service.retrain(userId);

        assertThat(result.driftApplied()).isFalse();
        verify(vectorStore, never()).upsertProfileEmbedding(any(), any(), any());
    }
}
