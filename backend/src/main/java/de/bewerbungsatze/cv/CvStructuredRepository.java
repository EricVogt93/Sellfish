package de.bewerbungsatze.cv;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CvStructuredRepository extends JpaRepository<CvStructured, UUID> {

    Optional<CvStructured> findByUserId(UUID userId);
}
