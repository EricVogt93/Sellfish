package de.sellfish.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.infisical")
public record InfisicalProperties(
        boolean enabled,
        String baseUrl,
        String clientId,
        String clientSecret,
        String projectId,
        String environment) {
}
