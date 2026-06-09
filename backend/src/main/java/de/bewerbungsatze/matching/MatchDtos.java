package de.bewerbungsatze.matching;

import de.bewerbungsatze.jobs.Job;

import java.time.Instant;
import java.util.UUID;

public final class MatchDtos {

    private MatchDtos() {
    }

    public record MatchResponse(
            UUID matchId,
            UUID jobId,
            String title,
            String company,
            String location,
            String url,
            String salaryRaw,
            Instant postedAt,
            double score,
            Integer rank,
            MatchStatus status,
            String scoreBreakdown) {

        public static MatchResponse of(JobMatch match, Job job) {
            return new MatchResponse(
                    match.getId(), job.getId(), job.getTitle(), job.getCompany(), job.getLocation(),
                    job.getUrl(), job.getSalaryRaw(), job.getPostedAt(), match.getScore(),
                    match.getRank(), match.getStatus(), match.getScoreBreakdown());
        }
    }

    public record StatusRequest(MatchStatus status) {
    }
}
