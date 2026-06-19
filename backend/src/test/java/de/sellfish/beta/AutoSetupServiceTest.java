package de.sellfish.beta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.sellfish.ai.LlmService;
import de.sellfish.ai.model.ChatRequest;
import de.sellfish.ai.model.ChatResult;
import de.sellfish.cv.CvStructured;
import de.sellfish.cv.CvStructuredRepository;
import de.sellfish.jobs.JobSearchService;
import de.sellfish.profile.ProfileRepository;
import de.sellfish.profile.ProfileService;
import de.sellfish.profile.UserPreferences;
import de.sellfish.profile.UserProfile;
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
class AutoSetupServiceTest {

    @Mock
    LlmService llmService;

    @Mock
    CvStructuredRepository cvRepository;

    @Mock
    ProfileRepository profileRepository;

    @Mock
    ProfileService profileService;

    @Mock
    JobSearchService jobSearchService;

    AutoSetupService service;

    @BeforeEach
    void setup() {
        service = new AutoSetupService(
                llmService, cvRepository, profileRepository, profileService, jobSearchService, new ObjectMapper());
    }

    @Test
    void returnsNoCvWhenCvMissing() {
        UUID userId = UUID.randomUUID();
        when(cvRepository.findByUserId(userId)).thenReturn(Optional.empty());

        AutoSetupService.SetupResult result = service.run(userId);

        assertThat(result.status()).isEqualTo("no_cv");
        verifyNoInteractions(llmService);
        verify(jobSearchService, never()).runForUser(any());
    }

    @Test
    void returnsParseFailedOnInvalidJson() {
        UUID userId = UUID.randomUUID();
        when(cvRepository.findByUserId(userId)).thenReturn(Optional.of(mock(CvStructured.class)));
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(llmService.chat(any(UUID.class), any(ChatRequest.class)))
                .thenReturn(new ChatResult("not json", "m", null, null));

        AutoSetupService.SetupResult result = service.run(userId);

        assertThat(result.status()).isEqualTo("parse_failed");
        verify(jobSearchService, never()).runForUser(any());
    }

    @Test
    void runPopulatesProfileAndTriggersSearch() {
        UUID userId = UUID.randomUUID();
        CvStructured cv = mock(CvStructured.class);
        when(cvRepository.findByUserId(userId)).thenReturn(Optional.of(cv));
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(llmService.chat(any(UUID.class), any(ChatRequest.class)))
                .thenReturn(new ChatResult(
                        """
                        {"headline":"Backend Dev","summary":"sum","location":"Berlin","remotePref":"REMOTE","salaryMin":80000,"titles":["Java Dev"],"keywords":["spring","kafka"]}""",
                        "m",
                        null,
                        null));
        UserProfile profile = new UserProfile(userId);
        when(profileService.getOrCreateProfile(userId)).thenReturn(profile);
        when(profileService.getOrCreatePreferences(userId)).thenReturn(new UserPreferences(userId));

        AutoSetupService.SetupResult result = service.run(userId);

        assertThat(result.status()).isEqualTo("ok");
        assertThat(result.data().headline()).isEqualTo("Backend Dev");
        verify(profileService).save(profile);
        verify(jobSearchService).runForUser(userId);
    }
}
