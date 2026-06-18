package de.sellfish.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface LlmProviderConfigRepository extends JpaRepository<LlmProviderConfig, UUID> {

    List<LlmProviderConfig> findByUserIdAndPurposeAndEnabledTrue(UUID userId, Purpose purpose);

    @Query("select c from LlmProviderConfig c where c.userId is null and c.purpose = ?1 and c.enabled = true")
    List<LlmProviderConfig> findGlobalByPurpose(Purpose purpose);

    @Query("select c from LlmProviderConfig c where c.userId is null")
    List<LlmProviderConfig> findAllGlobal();

    List<LlmProviderConfig> findByUserId(UUID userId);
}
