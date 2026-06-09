package de.bewerbungsatze.matching;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bewerbungsatze.jobs.Job;
import de.bewerbungsatze.jobs.JobRepository;
import de.bewerbungsatze.jobs.VectorStore;
import de.bewerbungsatze.profile.PreferencesRepository;
import de.bewerbungsatze.profile.ProfileRepository;
import de.bewerbungsatze.profile.UserPreferences;
import de.bewerbungsatze.profile.UserProfile;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchingServiceTest {

    private final ProfileRepository profileRepository = mock(ProfileRepository.class);
    private final PreferencesRepository preferencesRepository = mock(PreferencesRepository.class);
    private final JobRepository jobRepository = mock(JobRepository.class);
    private final JobMatchRepository matchRepository = mock(JobMatchRepository.class);
    private final UserRankingModelRepository rankingRepository = mock(UserRankingModelRepository.class);
    private final VectorStore vectorStore = mock(VectorStore.class);

    private final MatchingService service = new MatchingService(
            profileRepository, preferencesRepository, jobRepository, matchRepository,
            rankingRepository, vectorStore, new FeatureScorer(), new ObjectMapper());

    private Job job(String title, String company) {
        Job j = new Job("BA", "fp-" + UUID.randomUUID(), title);
        j.setCompany(company);
        return j;
    }

    @SuppressWarnings("unchecked")
    private List<JobMatch> capture() {
        ArgumentCaptor<List<JobMatch>> captor = ArgumentCaptor.forClass(List.class);
        verify(matchRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void ranksBySemanticAndFeatureScore() {
        UUID userId = UUID.randomUUID();
        UserPreferences prefs = new UserPreferences(userId);
        prefs.setDesiredTitles(new String[]{"Java Entwickler"});
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.of(prefs));
        when(rankingRepository.findFirstByUserIdOrderByVersionDesc(userId)).thenReturn(Optional.empty());

        Job good = job("Java Entwickler", "Acme");
        Job weak = job("Marketing Manager", "Beta");
        when(vectorStore.hasProfileEmbedding(userId)).thenReturn(true);
        when(vectorStore.similarJobsForUser(eq(userId), anyInt()))
                .thenReturn(List.of(
                        new VectorStore.SimilarJob(good.getId(), 0.9),
                        new VectorStore.SimilarJob(weak.getId(), 0.2)));
        when(jobRepository.findAllById(any())).thenReturn(List.of(good, weak));
        when(matchRepository.findByUserId(userId)).thenReturn(List.of());

        int count = service.recompute(userId);

        assertThat(count).isEqualTo(2);
        List<JobMatch> saved = capture();
        JobMatch goodMatch = saved.stream().filter(m -> m.getJobId().equals(good.getId())).findFirst().orElseThrow();
        JobMatch weakMatch = saved.stream().filter(m -> m.getJobId().equals(weak.getId())).findFirst().orElseThrow();
        assertThat(goodMatch.getScore()).isGreaterThan(weakMatch.getScore());
        assertThat(goodMatch.getRank()).isEqualTo(1);
        assertThat(goodMatch.getScoreBreakdown()).contains("semantic");
    }

    @Test
    void excludedCompanyIsSkipped() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = new UserProfile(userId);
        UserPreferences prefs = new UserPreferences(userId);
        prefs.setExcludedCompanies(new String[]{"EvilCorp"});
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.of(prefs));
        when(rankingRepository.findFirstByUserIdOrderByVersionDesc(userId)).thenReturn(Optional.empty());

        Job evil = job("Dev", "EvilCorp GmbH");
        when(vectorStore.hasProfileEmbedding(userId)).thenReturn(false);
        when(jobRepository.findTop500ByOrderByCreatedAtDesc()).thenReturn(List.of(evil));
        when(matchRepository.findByUserId(userId)).thenReturn(List.of());

        assertThat(service.recompute(userId)).isZero();
    }

    @Test
    void preservesExistingMatchIdentity() {
        UUID userId = UUID.randomUUID();
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(rankingRepository.findFirstByUserIdOrderByVersionDesc(userId)).thenReturn(Optional.empty());

        Job j = job("Dev", "Acme");
        JobMatch existing = new JobMatch(userId, j.getId());
        existing.setStatus(MatchStatus.SAVED);

        when(vectorStore.hasProfileEmbedding(userId)).thenReturn(false);
        when(jobRepository.findTop500ByOrderByCreatedAtDesc()).thenReturn(List.of(j));
        when(matchRepository.findByUserId(userId)).thenReturn(List.of(existing));

        service.recompute(userId);
        List<JobMatch> saved = capture();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getStatus()).isEqualTo(MatchStatus.SAVED);
    }
}
