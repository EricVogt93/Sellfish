package de.sellfish.jobs;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findByFingerprint(String fingerprint);

    boolean existsByFingerprint(String fingerprint);

    java.util.List<Job> findTop500ByOrderByCreatedAtDesc();
}
