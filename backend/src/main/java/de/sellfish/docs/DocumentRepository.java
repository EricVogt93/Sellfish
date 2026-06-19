package de.sellfish.docs;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Document> findByUserIdAndType(UUID userId, DocumentType type);

    Optional<Document> findByUserIdAndTypeAndPrimaryTrue(UUID userId, DocumentType type);
}
