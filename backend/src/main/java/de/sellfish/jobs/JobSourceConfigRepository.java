package de.sellfish.jobs;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobSourceConfigRepository extends JpaRepository<JobSourceConfig, UUID> {

    List<JobSourceConfig> findByEnabledTrue();

    java.util.Optional<JobSourceConfig> findByCode(String code);
}
