package de.sellfish.cv;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CvStructuredRepository extends JpaRepository<CvStructured, UUID> {

    Optional<CvStructured> findByUserId(UUID userId);
}
