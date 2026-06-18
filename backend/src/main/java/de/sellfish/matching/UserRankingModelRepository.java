package de.sellfish.matching;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRankingModelRepository extends JpaRepository<UserRankingModel, UUID> {

    Optional<UserRankingModel> findFirstByUserIdOrderByVersionDesc(UUID userId);
}
