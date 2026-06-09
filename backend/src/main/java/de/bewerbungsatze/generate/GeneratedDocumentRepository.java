package de.bewerbungsatze.generate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GeneratedDocumentRepository extends JpaRepository<GeneratedDocument, UUID> {

    List<GeneratedDocument> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<GeneratedDocument> findByUserIdAndJobMatchIdOrderByCreatedAtDesc(UUID userId, UUID jobMatchId);

    Optional<GeneratedDocument> findFirstByUserIdAndJobMatchIdAndTypeOrderByVersionDesc(
            UUID userId, UUID jobMatchId, GenerationType type);
}
