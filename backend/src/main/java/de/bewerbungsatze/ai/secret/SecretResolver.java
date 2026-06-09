package de.bewerbungsatze.ai.secret;

import de.bewerbungsatze.ai.LlmProviderConfig;
import de.bewerbungsatze.common.crypto.CryptoService;
import org.springframework.stereotype.Component;

/**
 * Ermittelt den effektiven API-Key einer Provider-Konfiguration:
 * verschlüsselter Per-User-Key vor Infisical-Referenz.
 */
@Component
public class SecretResolver {

    private final CryptoService cryptoService;
    private final InfisicalClient infisicalClient;

    public SecretResolver(CryptoService cryptoService, InfisicalClient infisicalClient) {
        this.cryptoService = cryptoService;
        this.infisicalClient = infisicalClient;
    }

    public String resolveApiKey(LlmProviderConfig config) {
        if (config.getKeyEnc() != null && !config.getKeyEnc().isBlank()) {
            return cryptoService.decrypt(config.getKeyEnc());
        }
        if (config.getKeyRef() != null && !config.getKeyRef().isBlank()) {
            return infisicalClient.getSecret(config.getKeyRef()).orElse(null);
        }
        return null;
    }
}
