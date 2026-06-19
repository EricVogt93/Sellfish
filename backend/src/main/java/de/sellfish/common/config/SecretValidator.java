package de.sellfish.common.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Fails startup if the JWT or crypto secret is still the dev default AND the
 * application is not running with a dev/test profile. This prevents accidental
 * production deployments with known-guessable secrets.
 */
@Configuration
public class SecretValidator {

    private static final Logger log = LoggerFactory.getLogger(SecretValidator.class);

    static final String DEV_JWT_SECRET = "Y2hhbmdlLW1lLXRoaXMtaXMtYS1kZXYtb25seS1zZWNyZXQtMzJieXRlcyE=";
    static final String DEV_CRYPTO_KEY = "ZGV2LW9ubHktbWFzdGVyLWtleS0zMi1ieXRlcy1vayE=";

    private final Environment env;

    public SecretValidator(Environment env) {
        this.env = env;
    }

    @PostConstruct
    void validate() {
        boolean isDev = env.matchesProfiles("dev", "test");
        String jwt = env.getProperty("app.security.jwt.secret", "");
        String crypto = env.getProperty("app.crypto.master-key", "");

        if (DEV_JWT_SECRET.equals(jwt)) {
            if (isDev) {
                log.warn("JWT secret is the dev default — acceptable in dev/test only.");
            } else {
                throw new IllegalStateException(
                        "JWT_SECRET is the insecure dev default. Set a real secret (openssl rand -base64 32) via environment variable before starting in production.");
            }
        }
        if (DEV_CRYPTO_KEY.equals(crypto)) {
            if (isDev) {
                log.warn("Crypto master key is the dev default — acceptable in dev/test only.");
            } else {
                throw new IllegalStateException(
                        "CRYPTO_MASTER_KEY is the insecure dev default. Set a real key (openssl rand -base64 32) via environment variable before starting in production.");
            }
        }
    }
}
