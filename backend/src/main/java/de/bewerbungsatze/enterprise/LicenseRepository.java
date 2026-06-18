package de.bewerbungsatze.enterprise;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LicenseRepository extends JpaRepository<LicenseEntity, UUID> {

    Optional<LicenseEntity> findTopByOrderByCreatedAtDesc();
}
