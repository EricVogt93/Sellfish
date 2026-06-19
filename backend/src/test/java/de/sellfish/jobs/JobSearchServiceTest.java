package de.sellfish.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.sellfish.cv.CvStructuredRepository;
import de.sellfish.jobs.port.JobQuery;
import de.sellfish.matching.MatchingService;
import de.sellfish.profile.PreferencesRepository;
import de.sellfish.profile.ProfileRepository;
import de.sellfish.profile.UserPreferences;
import de.sellfish.profile.UserProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JobSearchServiceTest {

    @Mock
    ProfileRepository profileRepository;

    @Mock
    PreferencesRepository preferencesRepository;

    @Mock
    CvStructuredRepository cvRepository;

    @Mock
    JobIngestionService ingestionService;

    @Mock
    JobEmbeddingService embeddingService;

    @Mock
    SearchRunRepository searchRunRepository;

    @Mock
    MatchingService matchRecomputer;

    JobSearchService service;

    @BeforeEach
    void setup() {
        service = new JobSearchService(
                profileRepository,
                preferencesRepository,
                cvRepository,
                ingestionService,
                embeddingService,
                searchRunRepository,
                new ObjectMapper(),
                matchRecomputer);
    }

    @Test
    void buildQueryWithNullInputsReturnsEmptyKeywords() {
        JobQuery q = service.buildQuery(null, null);
        assertThat(q.keywords()).isEmpty();
        assertThat(q.location()).isNull();
        assertThat(q.size()).isEqualTo(50);
    }

    @Test
    void buildQueryMergesTitlesAndKeywords() {
        UserProfile profile = new UserProfile(UUID.randomUUID());
        profile.setLocation("Berlin");
        profile.setRemotePref("REMOTE");
        UserPreferences prefs = new UserPreferences(UUID.randomUUID());
        prefs.setDesiredTitles(new String[] {"Java Dev", "Backend"});
        prefs.setKeywords(new String[] {"spring", "kafka"});

        JobQuery q = service.buildQuery(profile, prefs);
        assertThat(q.keywords()).contains("Java Dev", "Backend", "spring", "kafka");
        assertThat(q.location()).isEqualTo("Berlin");
        assertThat(q.remoteOnly()).isTrue();
    }

    @Test
    void profileTextCombinesProfilePrefsAndCv() {
        UserProfile profile = new UserProfile(UUID.randomUUID());
        profile.setHeadline("Senior Dev");
        UserPreferences prefs = new UserPreferences(UUID.randomUUID());
        prefs.setKeywords(new String[] {"java"});
        var cv = new de.sellfish.cv.CvStructured(UUID.randomUUID(), UUID.randomUUID());
        cv.setSkills("[\"spring\"]");

        String text = service.profileText(profile, prefs, cv);
        assertThat(text).contains("Senior Dev", "java", "spring");
    }

    @Test
    void runForUserSucceedsAndStoresStats() throws Exception {
        UUID userId = UUID.randomUUID();
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(cvRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(ingestionService.ingest(any(), any()))
                .thenReturn(new JobIngestionService.IngestStats(10, 5, List.of("ADZUNA")));
        when(matchRecomputer.recompute(userId)).thenReturn(5);
        when(searchRunRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SearchRun run = service.runForUser(userId);

        assertThat(run.getStatus()).isEqualTo("DONE");
        assertThat(run.getStats()).contains("\"fetched\":10").contains("\"matches\":5");
        verify(embeddingService).embedProfile(eq(userId), anyString());
    }

    @Test
    void runForUserHandlesFailure() {
        UUID userId = UUID.randomUUID();
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(cvRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(ingestionService.ingest(any(), any())).thenThrow(new RuntimeException("boom"));
        when(searchRunRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SearchRun run = service.runForUser(userId);
        assertThat(run.getStatus()).isEqualTo("FAILED");
        assertThat(run.getStats()).contains("boom");
    }
}
