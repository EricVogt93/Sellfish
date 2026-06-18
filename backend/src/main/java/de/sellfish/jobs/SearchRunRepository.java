package de.sellfish.jobs;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SearchRunRepository extends JpaRepository<SearchRun, UUID> {

    Page<SearchRun> findByUserIdOrderByStartedAtDesc(UUID userId, Pageable pageable);
}
