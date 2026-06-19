package de.sellfish.sso;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sso")
public record OidcProperties(boolean enabled, String redirectUri, List<ProviderConfig> providers) {
    public record ProviderConfig(
            String id,
            String name,
            String issuer,
            String clientId,
            String clientSecret,
            String authorizationUri,
            String tokenUri,
            String userinfoUri,
            String jwksUri,
            List<String> scopes,
            Map<String, String> domainWhitelist) {}
}
