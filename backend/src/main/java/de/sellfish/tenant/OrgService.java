package de.sellfish.tenant;

import de.sellfish.audit.AuditAction;
import de.sellfish.audit.AuditService;
import de.sellfish.common.error.ApiException;
import de.sellfish.enterprise.LicenseService;
import de.sellfish.users.User;
import de.sellfish.users.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrgService {

    private final OrganizationRepository orgRepository;
    private final OrgMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final LicenseService licenseService;
    private final AuditService auditService;

    public OrgService(
            OrganizationRepository orgRepository,
            OrgMemberRepository memberRepository,
            UserRepository userRepository,
            LicenseService licenseService,
            AuditService auditService) {
        this.orgRepository = orgRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.licenseService = licenseService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<Organization> myOrganizations(UUID userId) {
        return memberRepository.findByUserId(userId).stream()
                .map(m -> orgRepository.findById(m.getOrgId()).orElse(null))
                .filter(o -> o != null)
                .toList();
    }

    @Transactional
    public Organization create(UUID userId, String name, String slug) {
        if (!licenseService.isEnterpriseFeatureEnabled("multi-tenant")) {
            throw ApiException.badRequest("Multi-tenant is an enterprise feature");
        }
        if (orgRepository.existsBySlug(slug)) {
            throw ApiException.conflict("Organization with slug '" + slug + "' already exists");
        }
        Organization org = new Organization(name, slug);
        org = orgRepository.save(org);
        memberRepository.save(new OrganizationMember(org.getId(), userId, OrgMemberRole.OWNER));
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        user.setCurrentOrgId(org.getId());
        userRepository.save(user);
        auditService.record(
                userId, AuditAction.ORG_CREATE, "organization", org.getId().toString());
        return org;
    }

    @Transactional(readOnly = true)
    public List<OrganizationMember> members(UUID orgId, UUID requestingUserId) {
        requireOrgAccess(orgId, requestingUserId);
        return memberRepository.findByOrgId(orgId);
    }

    @Transactional
    public void switchContext(UUID userId, UUID orgId) {
        if (orgId != null) {
            memberRepository
                    .findByOrgIdAndUserId(orgId, userId)
                    .orElseThrow(() -> ApiException.notFound("Not a member of this organization"));
        }
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        user.setCurrentOrgId(orgId);
        userRepository.save(user);
    }

    @Transactional
    public OrganizationMember addMember(UUID orgId, UUID requestingUserId, UUID newUserId, OrgMemberRole role) {
        requireOrgRole(orgId, requestingUserId, OrgMemberRole.ADMIN);
        if (memberRepository.findByOrgIdAndUserId(orgId, newUserId).isPresent()) {
            throw ApiException.conflict("User is already a member");
        }
        var member = memberRepository.save(new OrganizationMember(orgId, newUserId, role));
        auditService.record(requestingUserId, AuditAction.USER_INVITE, "organization", orgId.toString());
        return member;
    }

    @Transactional
    public void removeMember(UUID orgId, UUID requestingUserId, UUID targetUserId) {
        requireOrgRole(orgId, requestingUserId, OrgMemberRole.ADMIN);
        OrganizationMember target = memberRepository
                .findByOrgIdAndUserId(orgId, targetUserId)
                .orElseThrow(() -> ApiException.notFound("Member not found"));
        if (target.getRole() == OrgMemberRole.OWNER) {
            throw ApiException.badRequest("Owner cannot be removed");
        }
        memberRepository.delete(target);
        auditService.record(requestingUserId, AuditAction.ORG_LEAVE, "organization", orgId.toString());
        User targetUser =
                userRepository.findById(targetUserId).orElseThrow(() -> ApiException.notFound("User not found"));
        if (orgId.equals(targetUser.getCurrentOrgId())) {
            targetUser.setCurrentOrgId(null);
            userRepository.save(targetUser);
        }
    }

    @Transactional(readOnly = true)
    public List<User> listOrgUsers(UUID orgId, UUID requestingUserId) {
        requireOrgAccess(orgId, requestingUserId);
        return memberRepository.findByOrgId(orgId).stream()
                .map(m -> userRepository.findById(m.getUserId()).orElse(null))
                .filter(u -> u != null)
                .toList();
    }

    private void requireOrgAccess(UUID orgId, UUID userId) {
        memberRepository
                .findByOrgIdAndUserId(orgId, userId)
                .orElseThrow(() -> ApiException.notFound("No access to this organization"));
    }

    private void requireOrgRole(UUID orgId, UUID userId, OrgMemberRole minRole) {
        OrganizationMember member = memberRepository
                .findByOrgIdAndUserId(orgId, userId)
                .orElseThrow(() -> ApiException.notFound("No access to this organization"));
        if (member.getRole().ordinal() > minRole.ordinal()) {
            throw ApiException.badRequest("Missing permission");
        }
    }
}
