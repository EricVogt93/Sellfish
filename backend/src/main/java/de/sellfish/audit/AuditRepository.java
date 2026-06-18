package de.sellfish.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditRepository extends JpaRepository<AuditEvent, UUID> {

    Page<AuditEvent> findByOrgIdAndUserIdOrderByTsDesc(UUID orgId, UUID userId, Pageable pageable);

    Page<AuditEvent> findByOrgIdOrderByTsDesc(UUID orgId, Pageable pageable);

    Page<AuditEvent> findByUserIdOrderByTsDesc(UUID userId, Pageable pageable);

    Page<AuditEvent> findAllByOrderByTsDesc(Pageable pageable);
}
