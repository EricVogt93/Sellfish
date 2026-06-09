package de.bewerbungsatze.jobs.source;

import java.time.Instant;

/**
 * Normalisierte Stellenanzeige, wie sie eine Quelle liefert (vor der Persistenz).
 */
public record RawJob(
        String sourceCode,
        String externalRef,
        String title,
        String company,
        String location,
        String remote,
        String description,
        String url,
        String salaryRaw,
        Instant postedAt,
        String raw) {
}
