package de.sellfish.ai;

import de.sellfish.ai.model.ResolvedModel;
import de.sellfish.ai.secret.SecretResolver;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Wählt für einen Nutzer und Zweck die passende Provider-Konfiguration:
 * eigene Konfiguration vor globaler, Default vor übrigen.
 */
@Component
public class ProviderResolver {

    private final LlmProviderConfigRepository repository;
    private final SecretResolver secretResolver;

    public ProviderResolver(LlmProviderConfigRepository repository, SecretResolver secretResolver) {
        this.repository = repository;
        this.secretResolver = secretResolver;
    }

    public ResolvedModel resolve(UUID userId, Purpose purpose) {
        LlmProviderConfig config = pickConfig(userId, purpose)
                .orElseThrow(() -> new LlmException(
                        "Keine aktive LLM-Konfiguration für Zweck " + purpose + " (weder nutzer- noch systemweit)"));
        return new ResolvedModel(
                config.getProvider(), config.getModel(), config.getBaseUrl(), secretResolver.resolveApiKey(config));
    }

    public Optional<LlmProviderConfig> pickConfig(UUID userId, Purpose purpose) {
        if (userId != null) {
            Optional<LlmProviderConfig> userConfig =
                    best(repository.findByUserIdAndPurposeAndEnabledTrue(userId, purpose));
            if (userConfig.isPresent()) {
                return userConfig;
            }
        }
        return best(repository.findGlobalByPurpose(purpose));
    }

    private Optional<LlmProviderConfig> best(List<LlmProviderConfig> configs) {
        return configs.stream()
                .min(Comparator.comparing(LlmProviderConfig::isDefault)
                        .reversed()
                        .thenComparing(
                                LlmProviderConfig::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
    }
}
