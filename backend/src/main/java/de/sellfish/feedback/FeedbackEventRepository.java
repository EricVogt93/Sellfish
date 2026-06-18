package de.sellfish.feedback;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeedbackEventRepository extends JpaRepository<FeedbackEvent, UUID> {

    List<FeedbackEvent> findByUserIdOrderByTsDesc(UUID userId);
}
