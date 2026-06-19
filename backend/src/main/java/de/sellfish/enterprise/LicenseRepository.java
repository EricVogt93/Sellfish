package de.sellfish.enterprise;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LicenseRepository extends JpaRepository<LicenseEntity, UUID> {

    Optional<LicenseEntity> findTopByOrderByCreatedAtDesc();
}
