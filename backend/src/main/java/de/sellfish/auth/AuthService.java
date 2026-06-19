package de.sellfish.auth;

import de.sellfish.audit.AuditAction;
import de.sellfish.audit.AuditService;
import de.sellfish.auth.dto.AuthDtos.LoginRequest;
import de.sellfish.auth.dto.AuthDtos.RefreshRequest;
import de.sellfish.auth.dto.AuthDtos.RegisterRequest;
import de.sellfish.auth.dto.AuthDtos.TokenResponse;
import de.sellfish.common.config.SecurityProperties;
import de.sellfish.common.error.ApiException;
import de.sellfish.sso.OidcService;
import de.sellfish.users.User;
import de.sellfish.users.UserRepository;
import io.jsonwebtoken.Claims;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SecurityProperties securityProperties;
    private final AuditService auditService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            SecurityProperties securityProperties,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.securityProperties = securityProperties;
        this.auditService = auditService;
    }

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw ApiException.conflict("E-Mail ist bereits registriert");
        }
        User user = new User(req.email().toLowerCase(), passwordEncoder.encode(req.password()));
        userRepository.save(user);
        auditService.record(user.getId(), AuditAction.REGISTER);
        return issueTokens(user);
    }

    public TokenResponse login(LoginRequest req) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        User user = userRepository
                .findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> new BadCredentialsException("Ungültige Anmeldedaten"));
        auditService.record(user.getId(), AuditAction.LOGIN);
        return issueTokens(user);
    }

    public TokenResponse refresh(RefreshRequest req) {
        Claims claims;
        try {
            claims = jwtService.parse(req.refreshToken());
        } catch (RuntimeException ex) {
            throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        if (!jwtService.isRefreshToken(claims)) {
            throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED, "No refresh token");
        }
        UUID userId = UUID.fromString(claims.getSubject());
        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Nutzer nicht gefunden"));
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse loginWithSso(OidcService.OidcUser oidcUser) {
        User user = userRepository
                .findByOidcSubjectAndOidcProvider(oidcUser.subject(), oidcUser.provider())
                .orElseGet(() -> {
                    User newUser = User.sso(oidcUser.email(), oidcUser.subject(), oidcUser.provider());
                    return userRepository.save(newUser);
                });
        auditService.record(user.getId(), AuditAction.SSO_LOGIN);
        return issueTokens(user);
    }

    private TokenResponse issueTokens(User user) {
        String access = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getCurrentOrgId());
        String refresh = jwtService.generateRefreshToken(user.getId());
        return new TokenResponse(access, refresh, "Bearer", securityProperties.accessTokenTtlMinutes() * 60);
    }

    public TokenResponse issueAccessToken(User user) {
        String access = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getCurrentOrgId());
        return new TokenResponse(access, null, "Bearer", securityProperties.accessTokenTtlMinutes() * 60);
    }
}
