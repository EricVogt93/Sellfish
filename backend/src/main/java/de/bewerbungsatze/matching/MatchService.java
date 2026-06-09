package de.bewerbungsatze.matching;

import de.bewerbungsatze.common.error.ApiException;
import de.bewerbungsatze.feedback.FeedbackEvent;
import de.bewerbungsatze.feedback.FeedbackEventRepository;
import de.bewerbungsatze.feedback.FeedbackType;
import de.bewerbungsatze.jobs.Job;
import de.bewerbungsatze.jobs.JobRepository;
import de.bewerbungsatze.matching.MatchDtos.MatchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MatchService {

    private final JobMatchRepository matchRepository;
    private final JobRepository jobRepository;
    private final FeedbackEventRepository feedbackRepository;

    public MatchService(JobMatchRepository matchRepository,
                        JobRepository jobRepository,
                        FeedbackEventRepository feedbackRepository) {
        this.matchRepository = matchRepository;
        this.jobRepository = jobRepository;
        this.feedbackRepository = feedbackRepository;
    }

    @Transactional(readOnly = true)
    public Page<MatchResponse> list(UUID userId, MatchStatus status, Pageable pageable) {
        Page<JobMatch> page = status == null
                ? matchRepository.findByUserIdOrderByScoreDesc(userId, pageable)
                : matchRepository.findByUserIdAndStatusOrderByScoreDesc(userId, status, pageable);
        Map<UUID, Job> jobs = loadJobs(page.getContent());
        return page.map(m -> MatchResponse.of(m, jobs.get(m.getJobId())));
    }

    @Transactional
    public MatchResponse updateStatus(UUID userId, UUID matchId, MatchStatus status) {
        JobMatch match = matchRepository.findById(matchId)
                .filter(m -> m.getUserId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Match nicht gefunden"));
        match.setStatus(status);
        matchRepository.save(match);

        feedbackRepository.save(new FeedbackEvent(userId, match.getJobId(), toFeedbackType(status)));

        Job job = jobRepository.findById(match.getJobId())
                .orElseThrow(() -> ApiException.notFound("Stelle nicht gefunden"));
        return MatchResponse.of(match, job);
    }

    private FeedbackType toFeedbackType(MatchStatus status) {
        return switch (status) {
            case SAVED -> FeedbackType.SAVE;
            case DISMISSED -> FeedbackType.DISMISS;
            case APPLIED -> FeedbackType.APPLY;
            case INTERVIEW, OFFER, REJECTED -> FeedbackType.OUTCOME;
            default -> FeedbackType.VIEW;
        };
    }

    private Map<UUID, Job> loadJobs(List<JobMatch> matches) {
        List<UUID> ids = matches.stream().map(JobMatch::getJobId).toList();
        return jobRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Job::getId, j -> j));
    }
}
