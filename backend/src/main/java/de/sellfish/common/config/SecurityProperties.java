package de.sellfish.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bindet die {@code app.security.jwt.*}-Konfiguration.
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record SecurityProperties(String secret, long accessTokenTtlMinutes, long refreshTokenTtlDays) {}
