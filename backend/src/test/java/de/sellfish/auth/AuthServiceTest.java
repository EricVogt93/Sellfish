package de.sellfish.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.sellfish.audit.AuditAction;
import de.sellfish.audit.AuditService;
import de.sellfish.auth.dto.AuthDtos.RefreshRequest;
import de.sellfish.auth.dto.AuthDtos.RegisterRequest;
import de.sellfish.auth.dto.AuthDtos.TokenResponse;
import de.sellfish.common.config.SecurityProperties;
import de.sellfish.common.error.ApiException;
import de.sellfish.users.User;
import de.sellfish.users.UserRepository;
import io.jsonwebtoken.Claims;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    JwtService jwtService;

    @Mock
    SecurityProperties securityProperties;

    @Mock
    AuditService auditService;

    @InjectMocks
    AuthService service;

    @BeforeEach
    void setup() {
        when(securityProperties.accessTokenTtlMinutes()).thenReturn(30L);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("dup@x.com")).thenReturn(true);
        assertThatThrownBy(() -> service.register(new RegisterRequest("dup@x.com", "pw")))
                .isInstanceOf(ApiException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerCreatesUserAndIssuesTokens() {
        when(userRepository.existsByEmailIgnoreCase("new@x.com")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("hash");
        when(jwtService.generateAccessToken(any(), eq("new@x.com"), any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh");

        TokenResponse response = service.register(new RegisterRequest("new@x.com", "pw"));

        verify(userRepository).save(any());
        verify(auditService).record(any(), eq(AuditAction.REGISTER));
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
    }

    @Test
    void refreshRejectsNonRefreshToken() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(UUID.randomUUID().toString());
        when(jwtService.parse("tok")).thenReturn(claims);
        when(jwtService.isRefreshToken(claims)).thenReturn(false);

        assertThatThrownBy(() -> service.refresh(new RefreshRequest("tok"))).isInstanceOf(ApiException.class);
    }

    @Test
    void refreshIssuesNewTokensForValidRefresh() {
        UUID userId = UUID.randomUUID();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(userId.toString());
        when(jwtService.parse("tok")).thenReturn(claims);
        when(jwtService.isRefreshToken(claims)).thenReturn(true);
        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("newaccess");
        when(jwtService.generateRefreshToken(any())).thenReturn("newrefresh");

        TokenResponse response = service.refresh(new RefreshRequest("tok"));
        assertThat(response.accessToken()).isEqualTo("newaccess");
    }
}
