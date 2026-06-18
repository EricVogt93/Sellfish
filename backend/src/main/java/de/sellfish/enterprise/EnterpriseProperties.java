package de.sellfish.enterprise;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.enterprise")
public record EnterpriseProperties(
        boolean enabled,
        String publicKey,
        Features features
) {
    public record Features(
            boolean sso,
            boolean multiTenant,
            boolean reports,
            boolean auditLog,
            boolean ha
    ) {}
}
