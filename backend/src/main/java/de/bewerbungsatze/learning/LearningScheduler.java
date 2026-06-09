package de.bewerbungsatze.learning;

import de.bewerbungsatze.users.User;
import de.bewerbungsatze.users.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodisches Re-Training der Ranking-Modelle aller Nutzer.
 */
@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class LearningScheduler {

    private static final Logger log = LoggerFactory.getLogger(LearningScheduler.class);

    private final UserRepository userRepository;
    private final SelfLearningService selfLearningService;

    public LearningScheduler(UserRepository userRepository, SelfLearningService selfLearningService) {
        this.userRepository = userRepository;
        this.selfLearningService = selfLearningService;
    }

    /** Wöchentlich, Montag 03:00 Uhr. */
    @Scheduled(cron = "${app.scheduling.learning-cron:0 0 3 * * MON}")
    public void retrainAll() {
        for (User user : userRepository.findAll()) {
            try {
                selfLearningService.retrain(user.getId());
            } catch (RuntimeException e) {
                log.warn("Re-Training für {} fehlgeschlagen: {}", user.getId(), e.getMessage());
            }
        }
    }
}
