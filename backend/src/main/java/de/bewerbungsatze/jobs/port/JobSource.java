package de.bewerbungsatze.jobs.port;

import java.util.List;
import java.util.Map;

/**
 * Eine Quelle von Stellenanzeigen (API, Scraper, …).
 */
public interface JobSource {

    /** Eindeutiger Code, passend zu {@code job_sources.code}. */
    String code();

    /**
     * @param query  abgeleitete Suchparameter
     * @param config quellenspezifische Konfiguration aus {@code job_sources.config}
     */
    List<RawJob> fetch(JobQuery query, Map<String, Object> config);
}
