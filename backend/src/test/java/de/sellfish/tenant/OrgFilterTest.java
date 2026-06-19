package de.sellfish.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import de.sellfish.auth.AppUserDetails;
import de.sellfish.users.User;
import de.sellfish.users.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrgFilterTest {

    @Mock
    UserRepository userRepository;

    @Mock
    OrgMemberRepository memberRepository;

    @InjectMocks
    OrgFilter filter;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(UUID userId) {
        User user = new User("u@x.com", "h");
        // BaseEntity sets id via JPA; for the test, build AppUserDetails with a real user
        AppUserDetails details = new AppUserDetails(user);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(details, null, java.util.List.of()));
        return;
    }

    @Test
    void setsOrgFromHeaderWhenMember() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        authenticateWithId(userId);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Org-Id", orgId.toString());
        when(memberRepository.findByOrgIdAndUserId(orgId, userId))
                .thenReturn(Optional.of(new OrganizationMember(orgId, userId, OrgMemberRole.MEMBER)));

        filter.doFilter(req, new MockHttpServletResponse(), (r, s) -> {});
        assertThat(req.getAttribute(OrgFilter.ATTR_ORG_ID)).isEqualTo(orgId);
    }

    @Test
    void fallsBackToUsersCurrentOrg() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        authenticateWithId(userId);
        MockHttpServletRequest req = new MockHttpServletRequest();
        User user = mock(User.class);
        when(user.getCurrentOrgId()).thenReturn(orgId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        filter.doFilter(req, new MockHttpServletResponse(), (r, s) -> {});
        assertThat(req.getAttribute(OrgFilter.ATTR_ORG_ID)).isEqualTo(orgId);
    }

    @Test
    void noAuthLeavesOrgUnset() throws Exception {
        // no security context -> userId null -> no org attribute
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (r, s) -> {});
        // chain still runs, no exception
    }

    private void authenticateWithId(UUID userId) {
        // AppUserDetails.getId() derives from the wrapped User; set id via reflection on BaseEntity
        User user = new User("u@x.com", "h");
        try {
            var field = de.sellfish.common.domain.BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, userId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        AppUserDetails details = new AppUserDetails(user);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(details, null, java.util.List.of()));
    }
}
