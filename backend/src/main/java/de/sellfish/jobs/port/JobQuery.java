package de.sellfish.jobs.port;

import java.util.List;

/**
 * Suchparameter, abgeleitet aus Profil & Präferenzen eines Nutzers.
 */
public record JobQuery(
        List<String> keywords,
        String location,
        Integer radiusKm,
        boolean remoteOnly,
        int size) {

    public String keywordString() {
        return keywords == null ? "" : String.join(" ", keywords);
    }
}
