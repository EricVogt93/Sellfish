package de.sellfish.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "org_member")
public class OrganizationMember {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrgMemberRole role = OrgMemberRole.MEMBER;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();

    protected OrganizationMember() {}

    public OrganizationMember(UUID orgId, UUID userId, OrgMemberRole role) {
        this.orgId = orgId;
        this.userId = userId;
        this.role = role;
    }

    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public UUID getUserId() { return userId; }
    public OrgMemberRole getRole() { return role; }
    public void setRole(OrgMemberRole role) { this.role = role; }
    public Instant getJoinedAt() { return joinedAt; }
}
