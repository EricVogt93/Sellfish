package de.sellfish.jobs;

import de.sellfish.jobs.port.JobQuery;
import de.sellfish.profile.UserPreferences;
import de.sellfish.profile.UserProfile;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JobSearchServiceTest {

    private final JobSearchService service = new JobSearchService(
            mock(de.sellfish.profile.ProfileRepository.class),
            mock(de.sellfish.profile.PreferencesRepository.class),
            mock(de.sellfish.cv.CvStructuredRepository.class),
            mock(JobIngestionService.class),
            mock(JobEmbeddingService.class),
            mock(SearchRunRepository.class),
            new com.fasterxml.jackson.databind.ObjectMapper(),
            mock(MatchRecomputer.class));

    @Test
    void buildsQueryFromTitlesKeywordsAndLocation() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = new UserProfile(userId);
        profile.setLocation("Berlin");
        profile.setRemotePref("REMOTE");
        UserPreferences prefs = new UserPreferences(userId);
        prefs.setDesiredTitles(new String[]{"Java Entwickler"});
        prefs.setKeywords(new String[]{"spring", "Java Entwickler"}); // Duplikat wird zusammengeführt

        JobQuery query = service.buildQuery(profile, prefs);

        assertThat(query.keywords()).containsExactly("Java Entwickler", "spring");
        assertThat(query.location()).isEqualTo("Berlin");
        assertThat(query.remoteOnly()).isTrue();
        assertThat(query.radiusKm()).isEqualTo(50);
    }

    @Test
    void buildsQueryWithoutProfile() {
        JobQuery query = service.buildQuery(null, null);
        assertThat(query.keywords()).isEmpty();
        assertThat(query.location()).isNull();
        assertThat(query.radiusKm()).isNull();
    }

    @Test
    void profileTextConcatenatesAvailableFields() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = new UserProfile(userId);
        profile.setHeadline("Senior Java Dev");
        UserPreferences prefs = new UserPreferences(userId);
        prefs.setKeywords(new String[]{"spring"});

        String text = service.profileText(profile, prefs, null);
        assertThat(text).contains("Senior Java Dev").contains("spring");
    }
}
