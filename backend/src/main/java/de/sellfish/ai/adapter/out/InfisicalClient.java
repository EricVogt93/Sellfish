package de.sellfish.ai.adapter.out;

import com.fasterxml.jackson.databind.JsonNode;
import de.sellfish.common.config.InfisicalProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Lädt zentrale Secrets aus Infisical via Universal-Auth (Machine Identity).
 * Bei {@code app.infisical.enabled=false} eine No-Op, die {@link Optional#empty()} liefert.
 */
@Component
public class InfisicalClient {

    private final InfisicalProperties props;
    private final RestClient client;

    private volatile String cachedToken;
    private volatile Instant tokenExpiry = Instant.EPOCH;

    public InfisicalClient(InfisicalProperties props, RestClient.Builder builder) {
        this.props = props;
        this.client = builder.baseUrl(props.baseUrl() == null ? "https://app.infisical.com" : props.baseUrl())
                .build();
    }

    public boolean isEnabled() {
        return props.enabled();
    }

    /**
     * @param ref Pfad in der Form {@code /folder/secret-name} oder {@code secret-name}.
     */
    public Optional<String> getSecret(String ref) {
        if (!props.enabled() || ref == null || ref.isBlank()) {
            return Optional.empty();
        }
        String path = ref.startsWith("/") ? ref : "/" + ref;
        int slash = path.lastIndexOf('/');
        String secretPath = slash <= 0 ? "/" : path.substring(0, slash);
        String secretName = path.substring(slash + 1);

        try {
            String token = accessToken();
            JsonNode response = client.get()
                    .uri(uri -> uri.path("/api/v3/secrets/raw/{name}")
                            .queryParam("workspaceId", props.projectId())
                            .queryParam("environment", props.environment())
                            .queryParam("secretPath", secretPath)
                            .build(secretName))
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                return Optional.empty();
            }
            String value = response.path("secret").path("secretValue").asText(null);
            return Optional.ofNullable(value);
        } catch (RestClientException e) {
            throw new IllegalStateException("Infisical-Secret '" + ref + "' nicht ladbar: " + e.getMessage(), e);
        }
    }

    private synchronized String accessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }
        JsonNode response = client.post()
                .uri("/api/v1/auth/universal-auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "clientId", props.clientId(),
                        "clientSecret", props.clientSecret()))
                .retrieve()
                .body(JsonNode.class);
        if (response == null || response.path("accessToken").isMissingNode()) {
            throw new IllegalStateException("Infisical-Login fehlgeschlagen");
        }
        cachedToken = response.get("accessToken").asText();
        long ttl = response.path("expiresIn").asLong(3600);
        // Sicherheitspuffer von 60 Sekunden.
        tokenExpiry = Instant.now().plusSeconds(Math.max(60, ttl - 60));
        return cachedToken;
    }
}
