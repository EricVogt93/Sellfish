package de.sellfish.ai.model;

import de.sellfish.ai.Provider;

/**
 * Vollständig aufgelöste Modell-Koordinaten inkl. (entschlüsseltem) API-Key.
 */
public record ResolvedModel(
        Provider provider,
        String model,
        String baseUrl,
        String apiKey) {
}
