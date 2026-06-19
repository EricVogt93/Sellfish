package de.sellfish.generate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.sellfish.ai.LlmService;
import de.sellfish.ai.model.ChatRequest;
import de.sellfish.ai.model.ChatResult;
import de.sellfish.jobs.Job;
import de.sellfish.jobs.JobRepository;
import de.sellfish.matching.JobMatch;
import de.sellfish.matching.JobMatchRepository;
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
class InterviewPrepServiceTest {

    @Mock
    LlmService llmService;

    @Mock
    JobMatchRepository matchRepository;

    @Mock
    JobRepository jobRepository;

    @InjectMocks
    InterviewPrepService service;

    @Test
    void generateQuestionsReturnsContent() {
        UUID userId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        JobMatch match = mock(JobMatch.class);
        when(match.getJobId()).thenReturn(jobId);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        Job job = mock(Job.class);
        when(job.getTitle()).thenReturn("Java Dev");
        when(job.getCompany()).thenReturn("Acme");
        when(job.getDescription()).thenReturn("Spring");
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(llmService.chat(any(UUID.class), any(ChatRequest.class)))
                .thenReturn(new ChatResult("1. Question one\n2. Question two", "m", null, null));

        String result = service.generateQuestions(userId, matchId);
        assertThat(result).contains("Question one");
    }

    @Test
    void generateQuestionsThrowsWhenMatchMissing() {
        when(matchRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.generateQuestions(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void companyResearchReturnsMessageWhenNoCompany() {
        UUID matchId = UUID.randomUUID();
        JobMatch match = mock(JobMatch.class);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        Job job = mock(Job.class);
        when(job.getCompany()).thenReturn(null);
        when(jobRepository.findById(any())).thenReturn(Optional.of(job));

        String result = service.generateCompanyResearch(UUID.randomUUID(), matchId);
        assertThat(result).contains("No company name");
        verifyNoInteractions(llmService);
    }

    @Test
    void companyResearchUsesLlmWhenCompanyPresent() {
        UUID matchId = UUID.randomUUID();
        JobMatch match = mock(JobMatch.class);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        Job job = mock(Job.class);
        when(job.getCompany()).thenReturn("Acme");
        when(job.getDescription()).thenReturn("Build things");
        when(jobRepository.findById(any())).thenReturn(Optional.of(job));
        when(llmService.chat(any(UUID.class), any(ChatRequest.class)))
                .thenReturn(new ChatResult("Acme is a tech company.", "m", null, null));

        assertThat(service.generateCompanyResearch(UUID.randomUUID(), matchId)).contains("Acme is a tech");
    }
}
