package de.sellfish.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrgMemberRepository extends JpaRepository<OrganizationMember, UUID> {

    List<OrganizationMember> findByUserId(UUID userId);

    List<OrganizationMember> findByOrgId(UUID orgId);

    Optional<OrganizationMember> findByOrgIdAndUserId(UUID orgId, UUID userId);

    long countByOrgId(UUID orgId);

    void deleteByOrgIdAndUserId(UUID orgId, UUID userId);
}
