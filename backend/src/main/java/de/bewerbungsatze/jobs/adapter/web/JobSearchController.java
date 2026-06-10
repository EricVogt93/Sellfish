package de.bewerbungsatze.jobs.adapter.web;
import de.bewerbungsatze.jobs.*;

import de.bewerbungsatze.common.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobSearchController {

    private final JobSearchService jobSearchService;
    private final SearchRunRepository searchRunRepository;

    public JobSearchController(JobSearchService jobSearchService,
                              SearchRunRepository searchRunRepository) {
        this.jobSearchService = jobSearchService;
        this.searchRunRepository = searchRunRepository;
    }

    public record RunResponse(UUID id, String status, Instant startedAt, Instant finishedAt, String stats) {
        static RunResponse from(SearchRun run) {
            return new RunResponse(run.getId(), run.getStatus(), run.getStartedAt(),
                    run.getFinishedAt(), run.getStats());
        }
    }

    @PostMapping("/search")
    public RunResponse search() {
        return RunResponse.from(jobSearchService.runForUser(CurrentUser.id()));
    }

    @GetMapping("/runs")
    public Page<RunResponse> runs(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return searchRunRepository
                .findByUserIdOrderByStartedAtDesc(CurrentUser.id(), PageRequest.of(page, Math.min(size, 100)))
                .map(RunResponse::from);
    }
}
