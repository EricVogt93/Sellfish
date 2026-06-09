package de.bewerbungsatze.matching;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobMatchRepository extends JpaRepository<JobMatch, UUID> {

    Optional<JobMatch> findByUserIdAndJobId(UUID userId, UUID jobId);

    Page<JobMatch> findByUserIdOrderByScoreDesc(UUID userId, Pageable pageable);

    Page<JobMatch> findByUserIdAndStatusOrderByScoreDesc(UUID userId, MatchStatus status, Pageable pageable);

    List<JobMatch> findByUserId(UUID userId);

    List<JobMatch> findByUserIdAndStatusIn(UUID userId, List<MatchStatus> statuses);
}
