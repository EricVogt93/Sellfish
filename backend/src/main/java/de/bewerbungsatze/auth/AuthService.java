package de.bewerbungsatze.auth;

import de.bewerbungsatze.auth.dto.AuthDtos.LoginRequest;
import de.bewerbungsatze.auth.dto.AuthDtos.RefreshRequest;
import de.bewerbungsatze.auth.dto.AuthDtos.RegisterRequest;
import de.bewerbungsatze.auth.dto.AuthDtos.TokenResponse;
import de.bewerbungsatze.common.config.SecurityProperties;
import de.bewerbungsatze.common.error.ApiException;
import de.bewerbungsatze.users.User;
import de.bewerbungsatze.users.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SecurityProperties securityProperties;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       SecurityProperties securityProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.securityProperties = securityProperties;
    }

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw ApiException.conflict("E-Mail ist bereits registriert");
        }
        User user = new User(req.email().toLowerCase(), passwordEncoder.encode(req.password()));
        userRepository.save(user);
        return issueTokens(user);
    }

    public TokenResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        User user = userRepository.findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> new BadCredentialsException("Ungültige Anmeldedaten"));
        return issueTokens(user);
    }

    public TokenResponse refresh(RefreshRequest req) {
        Claims claims;
        try {
            claims = jwtService.parse(req.refreshToken());
        } catch (RuntimeException ex) {
            throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Ungültiges Refresh-Token");
        }
        if (!jwtService.isRefreshToken(claims)) {
            throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Kein Refresh-Token");
        }
        UUID userId = UUID.fromString(claims.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Nutzer nicht gefunden"));
        return issueTokens(user);
    }

    private TokenResponse issueTokens(User user) {
        String access = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refresh = jwtService.generateRefreshToken(user.getId());
        return new TokenResponse(
                access,
                refresh,
                "Bearer",
                securityProperties.accessTokenTtlMinutes() * 60);
    }
}
