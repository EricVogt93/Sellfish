package de.sellfish.jobs;

import de.sellfish.users.User;
import de.sellfish.users.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodischer Suchlauf für alle aktiven Nutzer. Abschaltbar via {@code app.scheduling.enabled=false}.
 */
@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class JobSearchScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobSearchScheduler.class);

    private final UserRepository userRepository;
    private final JobSearchService jobSearchService;

    public JobSearchScheduler(UserRepository userRepository, JobSearchService jobSearchService) {
        this.userRepository = userRepository;
        this.jobSearchService = jobSearchService;
    }

    /** Täglich um 04:00 Uhr. */
    @Scheduled(cron = "${app.scheduling.search-cron:0 0 4 * * *}")
    public void runDailySearches() {
        for (User user : userRepository.findAll()) {
            try {
                jobSearchService.runForUser(user.getId());
            } catch (RuntimeException e) {
                log.warn("Geplanter Suchlauf für {} fehlgeschlagen: {}", user.getId(), e.getMessage());
            }
        }
    }
}
