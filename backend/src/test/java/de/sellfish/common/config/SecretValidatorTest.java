package de.sellfish.common.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

class SecretValidatorTest {

    private Environment envWith(String jwtSecret, String cryptoKey, String... profiles) {
        Environment env = mock(Environment.class);
        when(env.getProperty("app.security.jwt.secret", "")).thenReturn(jwtSecret);
        when(env.getProperty("app.crypto.master-key", "")).thenReturn(cryptoKey);
        boolean isDev = profiles.length > 0 && (profiles[0].equals("dev") || profiles[0].equals("test"));
        when(env.matchesProfiles("dev", "test")).thenReturn(isDev);
        return env;
    }

    @Test
    void allowsDevSecretsInDevProfile() {
        Environment env = envWith(SecretValidator.DEV_JWT_SECRET, SecretValidator.DEV_CRYPTO_KEY, "dev");
        assertThatCode(() -> new SecretValidator(env).validate()).doesNotThrowAnyException();
    }

    @Test
    void allowsRealSecretsInProdProfile() {
        Environment env = envWith("a-real-secret-base64-32-bytes-ok!!", "another-real-key-base64-ok!", "prod");
        assertThatCode(() -> new SecretValidator(env).validate()).doesNotThrowAnyException();
    }

    @Test
    void failsOnDevJwtSecretWithoutDevProfile() {
        Environment env = envWith(SecretValidator.DEV_JWT_SECRET, "real-crypto-key-ok!", "prod");
        assertThatThrownBy(() -> new SecretValidator(env).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void failsOnDevCryptoKeyWithoutDevProfile() {
        Environment env = envWith("real-jwt-secret-ok!", SecretValidator.DEV_CRYPTO_KEY, "prod");
        assertThatThrownBy(() -> new SecretValidator(env).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CRYPTO_MASTER_KEY");
    }

    @Test
    void failsOnBothDevSecretsWithoutDevProfile() {
        Environment env = envWith(SecretValidator.DEV_JWT_SECRET, SecretValidator.DEV_CRYPTO_KEY);
        assertThatThrownBy(() -> new SecretValidator(env).validate()).isInstanceOf(IllegalStateException.class);
    }
}
