package de.sellfish.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.sellfish.audit.AuditAction;
import de.sellfish.audit.AuditService;
import de.sellfish.common.error.ApiException;
import de.sellfish.enterprise.LicenseService;
import de.sellfish.users.User;
import de.sellfish.users.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrgServiceTest {

    @Mock
    OrganizationRepository orgRepository;

    @Mock
    OrgMemberRepository memberRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    LicenseService licenseService;

    @Mock
    AuditService auditService;

    @InjectMocks
    OrgService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID orgId = UUID.randomUUID();

    @BeforeEach
    void licenseOn() {
        when(licenseService.isEnterpriseFeatureEnabled("multi-tenant")).thenReturn(true);
    }

    @Test
    void myOrganizationsFiltersNulls() {
        OrganizationMember m = new OrganizationMember(orgId, userId, OrgMemberRole.OWNER);
        when(memberRepository.findByUserId(userId)).thenReturn(List.of(m));
        when(orgRepository.findById(orgId)).thenReturn(Optional.of(new Organization("Acme", "acme")));
        assertThat(service.myOrganizations(userId)).hasSize(1);
    }

    @Test
    void createRejectsWhenFeatureDisabled() {
        when(licenseService.isEnterpriseFeatureEnabled("multi-tenant")).thenReturn(false);
        assertThatThrownBy(() -> service.create(userId, "Acme", "acme")).isInstanceOf(ApiException.class);
        verify(orgRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateSlug() {
        when(orgRepository.existsBySlug("acme")).thenReturn(true);
        assertThatThrownBy(() -> service.create(userId, "Acme", "acme")).isInstanceOf(ApiException.class);
    }

    @Test
    void createSucceedsAndAddsOwner() {
        Organization saved = new Organization("Acme", "acme");
        when(orgRepository.existsBySlug("acme")).thenReturn(false);
        when(orgRepository.save(any())).thenReturn(saved);
        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Organization result = service.create(userId, "Acme", "acme");

        assertThat(result).isSameAs(saved);
        verify(memberRepository).save(any(OrganizationMember.class));
        verify(userRepository).save(user);
        verify(auditService).record(eq(userId), eq(AuditAction.ORG_CREATE), eq("organization"), anyString());
    }

    @Test
    void switchContextRejectsNonMember() {
        when(memberRepository.findByOrgIdAndUserId(orgId, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.switchContext(userId, orgId)).isInstanceOf(ApiException.class);
    }

    @Test
    void switchContextToNullClears() {
        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        service.switchContext(userId, null);
        verify(user).setCurrentOrgId(null);
        verify(memberRepository, never()).findByOrgIdAndUserId(any(), any());
    }

    @Test
    void addMemberRequiresAdmin() {
        when(memberRepository.findByOrgIdAndUserId(orgId, userId))
                .thenReturn(Optional.of(new OrganizationMember(orgId, userId, OrgMemberRole.MEMBER)));
        assertThatThrownBy(() -> service.addMember(orgId, userId, UUID.randomUUID(), OrgMemberRole.MEMBER))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void addMemberRejectsDuplicate() {
        UUID newcomer = UUID.randomUUID();
        when(memberRepository.findByOrgIdAndUserId(orgId, userId))
                .thenReturn(Optional.of(new OrganizationMember(orgId, userId, OrgMemberRole.ADMIN)));
        when(memberRepository.findByOrgIdAndUserId(orgId, newcomer))
                .thenReturn(Optional.of(new OrganizationMember(orgId, newcomer, OrgMemberRole.MEMBER)));
        assertThatThrownBy(() -> service.addMember(orgId, userId, newcomer, OrgMemberRole.MEMBER))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void addMemberSucceedsAsAdmin() {
        UUID newcomer = UUID.randomUUID();
        when(memberRepository.findByOrgIdAndUserId(orgId, userId))
                .thenReturn(Optional.of(new OrganizationMember(orgId, userId, OrgMemberRole.ADMIN)));
        when(memberRepository.findByOrgIdAndUserId(orgId, newcomer)).thenReturn(Optional.empty());
        service.addMember(orgId, userId, newcomer, OrgMemberRole.MEMBER);
        verify(memberRepository).save(any(OrganizationMember.class));
        verify(auditService).record(eq(userId), eq(AuditAction.USER_INVITE), anyString(), anyString());
    }

    @Test
    void removeMemberProtectsOwner() {
        UUID target = UUID.randomUUID();
        OrganizationMember owner = new OrganizationMember(orgId, target, OrgMemberRole.OWNER);
        when(memberRepository.findByOrgIdAndUserId(orgId, userId))
                .thenReturn(Optional.of(new OrganizationMember(orgId, userId, OrgMemberRole.ADMIN)));
        when(memberRepository.findByOrgIdAndUserId(orgId, target)).thenReturn(Optional.of(owner));
        assertThatThrownBy(() -> service.removeMember(orgId, userId, target)).isInstanceOf(ApiException.class);
        verify(memberRepository, never()).delete(any());
    }

    @Test
    void membersRequiresAccess() {
        when(memberRepository.findByOrgIdAndUserId(orgId, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.members(orgId, userId)).isInstanceOf(ApiException.class);
    }
}
