package de.sellfish.matching;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRankingModelRepository extends JpaRepository<UserRankingModel, UUID> {

    Optional<UserRankingModel> findFirstByUserIdOrderByVersionDesc(UUID userId);
}
