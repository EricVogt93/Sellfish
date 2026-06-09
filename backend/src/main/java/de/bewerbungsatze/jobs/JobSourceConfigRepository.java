package de.bewerbungsatze.jobs;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobSourceConfigRepository extends JpaRepository<JobSourceConfig, UUID> {

    List<JobSourceConfig> findByEnabledTrue();

    java.util.Optional<JobSourceConfig> findByCode(String code);
}
