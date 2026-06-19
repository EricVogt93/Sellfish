package de.sellfish.feedback;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackEventRepository extends JpaRepository<FeedbackEvent, UUID> {

    List<FeedbackEvent> findByUserIdOrderByTsDesc(UUID userId);
}
