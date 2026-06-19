package de.sellfish.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.sellfish.common.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    // 32-byte HMAC key, base64
    private static final String SECRET = Base64.getEncoder().encodeToString(new byte[32]);

    private final JwtService jwt = new JwtService(new SecurityProperties(SECRET, 30, 14));

    @Test
    void accessTokenRoundTripAndClaims() {
        UUID userId = UUID.randomUUID();
        String token = jwt.generateAccessToken(userId, "user@example.com");

        Claims claims = jwt.parse(token);
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
        assertThat(jwt.isAccessToken(claims)).isTrue();
        assertThat(jwt.isRefreshToken(claims)).isFalse();
    }

    @Test
    void accessTokenIncludesOrgIdWhenProvided() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Claims claims = jwt.parse(jwt.generateAccessToken(userId, "u@x.com", orgId));
        assertThat(claims.get("orgId", String.class)).isEqualTo(orgId.toString());
    }

    @Test
    void accessTokenOmitsOrgIdWhenNull() {
        Claims claims = jwt.parse(jwt.generateAccessToken(UUID.randomUUID(), "u@x.com", null));
        assertThat(claims.get("orgId")).isNull();
    }

    @Test
    void refreshTokenTypeIsRefresh() {
        UUID userId = UUID.randomUUID();
        Claims claims = jwt.parse(jwt.generateRefreshToken(userId));
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.isRefreshToken(claims)).isTrue();
        assertThat(jwt.isAccessToken(claims)).isFalse();
    }

    @Test
    void parseRejectsTamperedToken() {
        String token = jwt.generateAccessToken(UUID.randomUUID(), "u@x.com");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThatThrownBy(() -> jwt.parse(tampered)).isInstanceOf(Exception.class);
    }

    @Test
    void parseRejectsForeignToken() {
        byte[] foreignKey = new byte[32];
        java.util.Arrays.fill(foreignKey, (byte) 1);
        JwtService other =
                new JwtService(new SecurityProperties(Base64.getEncoder().encodeToString(foreignKey), 30, 14));
        String foreign = other.generateAccessToken(UUID.randomUUID(), "x@y.com");
        assertThatThrownBy(() -> jwt.parse(foreign)).isInstanceOf(Exception.class);
    }
}
