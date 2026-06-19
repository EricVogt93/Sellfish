package de.sellfish.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.sellfish.ai.LlmService;
import de.sellfish.ai.model.ChatRequest;
import de.sellfish.ai.model.ChatResult;
import de.sellfish.jobs.Job;
import de.sellfish.profile.UserPreferences;
import de.sellfish.profile.UserProfile;
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
class JobValidationServiceTest {

    @Mock
    LlmService llmService;

    @InjectMocks
    JobValidationService service;

    @Test
    void parseScoreExtractsNumber() {
        assertThat(JobValidationService.parseScore("85")).isCloseTo(0.85, within(0.001));
        assertThat(JobValidationService.parseScore("Score: 92 points")).isCloseTo(0.92, within(0.001));
    }

    @Test
    void parseScoreReturnsDefaultForGarbage() {
        assertThat(JobValidationService.parseScore("not a number")).isCloseTo(0.5, within(0.001));
    }

    @Test
    void parseScoreIgnoresOutOfRange() {
        assertThat(JobValidationService.parseScore("150")).isCloseTo(0.5, within(0.001));
    }

    @Test
    void parseScoreClampsToUnit() {
        assertThat(JobValidationService.parseScore("100")).isCloseTo(1.0, within(0.001));
        assertThat(JobValidationService.parseScore("0")).isCloseTo(0.0, within(0.001));
    }

    @Test
    void relevanceScoreUsesLlmResponse() {
        when(llmService.chat(any(UUID.class), any(ChatRequest.class)))
                .thenReturn(new ChatResult("78", "m", null, null));
        Job job = mock(Job.class);
        job.setTitle("Dev");
        double score = service.relevanceScore(UUID.randomUUID(), job, new UserProfile(UUID.randomUUID()), null);
        assertThat(score).isCloseTo(0.78, within(0.001));
    }

    @Test
    void relevanceScoreFallsBackOnLlmError() {
        when(llmService.chat(any(UUID.class), any(ChatRequest.class))).thenThrow(new RuntimeException("down"));
        double score = service.relevanceScore(
                UUID.randomUUID(), mock(Job.class), null, new UserPreferences(UUID.randomUUID()));
        assertThat(score).isCloseTo(0.5, within(0.001));
    }

    @Test
    void isLegitimateParsesYesAnswer() {
        when(llmService.chat(any(UUID.class), any(ChatRequest.class)))
                .thenReturn(new ChatResult("yes", "m", null, null));
        assertThat(service.isLegitimate(UUID.randomUUID(), mock(Job.class))).isTrue();
    }

    @Test
    void isLegitimateReturnsTrueOnFallback() {
        when(llmService.chat(any(UUID.class), any(ChatRequest.class))).thenThrow(new RuntimeException("err"));
        // failure defaults to legitimate (avoid blocking real jobs on transient errors)
        assertThat(service.isLegitimate(UUID.randomUUID(), mock(Job.class))).isTrue();
    }
}
