package de.sellfish.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public final class LlmConfigDtos {

    private LlmConfigDtos() {}

    public record ConfigRequest(
            @NotNull Provider provider,
            @NotBlank String model,
            String baseUrl,
            String keyRef,
            String apiKey,
            @NotNull Purpose purpose,
            Boolean isDefault,
            Boolean enabled,
            String params) {}

    public record ConfigResponse(
            UUID id,
            Provider provider,
            String model,
            String baseUrl,
            String keyRef,
            boolean hasKey,
            Purpose purpose,
            boolean isDefault,
            boolean enabled,
            String params) {

        public static ConfigResponse from(LlmProviderConfig c) {
            boolean hasKey = (c.getKeyEnc() != null && !c.getKeyEnc().isBlank())
                    || (c.getKeyRef() != null && !c.getKeyRef().isBlank());
            return new ConfigResponse(
                    c.getId(),
                    c.getProvider(),
                    c.getModel(),
                    c.getBaseUrl(),
                    c.getKeyRef(),
                    hasKey,
                    c.getPurpose(),
                    c.isDefault(),
                    c.isEnabled(),
                    c.getParams());
        }
    }

    public record TestResult(boolean ok, String message) {}
}
