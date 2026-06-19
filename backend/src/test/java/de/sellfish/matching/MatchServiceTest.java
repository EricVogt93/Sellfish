package de.sellfish.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.sellfish.common.error.ApiException;
import de.sellfish.feedback.FeedbackEventRepository;
import de.sellfish.feedback.FeedbackType;
import de.sellfish.jobs.Job;
import de.sellfish.jobs.JobRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatchServiceTest {

    @Mock
    JobMatchRepository matchRepository;

    @Mock
    JobRepository jobRepository;

    @Mock
    FeedbackEventRepository feedbackRepository;

    @InjectMocks
    MatchService service;

    @Test
    void listWithoutStatusOrdersByScore() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        when(matchRepository.findByUserIdOrderByScoreDesc(userId, pageable)).thenReturn(new PageImpl<>(List.of()));

        assertThat(service.list(userId, null, pageable).getContent()).isEmpty();
        verify(matchRepository, never()).findByUserIdAndStatusOrderByScoreDesc(any(), any(), any());
    }

    @Test
    void listWithStatusUsesStatusQuery() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        when(matchRepository.findByUserIdAndStatusOrderByScoreDesc(userId, MatchStatus.NEW, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        service.list(userId, MatchStatus.NEW, pageable);
        verify(matchRepository).findByUserIdAndStatusOrderByScoreDesc(userId, MatchStatus.NEW, pageable);
        verify(matchRepository, never()).findByUserIdOrderByScoreDesc(any(), any());
    }

    @Test
    void updateStatusThrowsWhenNotFound() {
        UUID userId = UUID.randomUUID();
        when(matchRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateStatus(userId, UUID.randomUUID(), MatchStatus.SAVED))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void updateStatusThrowsForOtherUsersMatch() {
        UUID userId = UUID.randomUUID();
        JobMatch others = mock(JobMatch.class);
        when(others.getUserId()).thenReturn(UUID.randomUUID()); // different user
        when(matchRepository.findById(any())).thenReturn(Optional.of(others));
        assertThatThrownBy(() -> service.updateStatus(userId, UUID.randomUUID(), MatchStatus.SAVED))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void updateStatusSavesMatchAndFeedback() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        JobMatch match = mock(JobMatch.class);
        when(match.getUserId()).thenReturn(userId);
        when(match.getJobId()).thenReturn(jobId);
        when(matchRepository.findById(any())).thenReturn(Optional.of(match));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(mock(Job.class)));

        service.updateStatus(userId, UUID.randomUUID(), MatchStatus.SAVED);

        verify(match).setStatus(MatchStatus.SAVED);
        verify(matchRepository).save(match);
        verify(feedbackRepository).save(argThat(f -> f.getType() == FeedbackType.SAVE));
    }

    @Test
    void updateStatusAppliedMapsToApplyFeedback() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        JobMatch match = mock(JobMatch.class);
        when(match.getUserId()).thenReturn(userId);
        when(match.getJobId()).thenReturn(jobId);
        when(matchRepository.findById(any())).thenReturn(Optional.of(match));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(mock(Job.class)));

        service.updateStatus(userId, UUID.randomUUID(), MatchStatus.APPLIED);
        verify(feedbackRepository).save(argThat(f -> f.getType() == FeedbackType.APPLY));
    }
}
