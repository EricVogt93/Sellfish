package de.bewerbungsatze.ai.model;

import de.bewerbungsatze.ai.Provider;

/**
 * Vollständig aufgelöste Modell-Koordinaten inkl. (entschlüsseltem) API-Key.
 */
public record ResolvedModel(
        Provider provider,
        String model,
        String baseUrl,
        String apiKey) {
}
