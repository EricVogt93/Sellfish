package de.sellfish.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.jsonwebtoken.Claims;
import java.util.List;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthenticationFilterTest {

    @Mock
    JwtService jwtService;

    @Mock
    AppUserDetailsService userDetailsService;

    @InjectMocks
    JwtAuthenticationFilter filter;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private UserDetails enabledUser() {
        return new User("user@x.com", "pw", List.of());
    }

    @Test
    void noHeaderContinuesWithoutAuth() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        boolean[] chainRan = {false};
        filter.doFilter(req, new MockHttpServletResponse(), (request, response) -> chainRan[0] = true);
        assertThat(chainRan[0]).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void validTokenSetsAuthentication() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer tok");
        Claims claims = mock(Claims.class);
        when(claims.get("email", String.class)).thenReturn("user@x.com");
        when(claims.get("orgId", String.class)).thenReturn(UUID.randomUUID().toString());
        when(jwtService.parse("tok")).thenReturn(claims);
        when(jwtService.isAccessToken(claims)).thenReturn(true);
        when(userDetailsService.loadUserByUsername("user@x.com")).thenReturn(enabledUser());

        filter.doFilter(req, new MockHttpServletResponse(), (request, response) -> {});

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("user@x.com");
        assertThat(req.getAttribute("ba.orgId")).isNotNull();
    }

    @Test
    void invalidTokenClearsContext() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer bad");
        when(jwtService.parse("bad")).thenThrow(new io.jsonwebtoken.JwtException("bad"));

        filter.doFilter(req, new MockHttpServletResponse(), (request, response) -> {});
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void refreshTokenDoesNotAuthenticate() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer tok");
        Claims claims = mock(Claims.class);
        when(jwtService.parse("tok")).thenReturn(claims);
        when(jwtService.isAccessToken(claims)).thenReturn(false);

        filter.doFilter(req, new MockHttpServletResponse(), (request, response) -> {});
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void disabledUserDoesNotAuthenticate() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer tok");
        Claims claims = mock(Claims.class);
        when(claims.get("email", String.class)).thenReturn("user@x.com");
        when(jwtService.parse("tok")).thenReturn(claims);
        when(jwtService.isAccessToken(claims)).thenReturn(true);
        // disabled account -> User.withUsername(...).disabled(true)
        UserDetails disabled = User.withUsername("user@x.com")
                .password("pw")
                .disabled(true)
                .authorities(List.of())
                .build();
        when(userDetailsService.loadUserByUsername("user@x.com")).thenReturn(disabled);

        filter.doFilter(req, new MockHttpServletResponse(), (request, response) -> {});
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
