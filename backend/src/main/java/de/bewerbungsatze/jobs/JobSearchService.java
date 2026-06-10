package de.bewerbungsatze.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bewerbungsatze.cv.CvStructured;
import de.bewerbungsatze.cv.CvStructuredRepository;
import de.bewerbungsatze.jobs.port.JobQuery;
import de.bewerbungsatze.profile.PreferencesRepository;
import de.bewerbungsatze.profile.ProfileRepository;
import de.bewerbungsatze.profile.UserPreferences;
import de.bewerbungsatze.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestriert einen Suchlauf für einen Nutzer: Query bauen, Profil-Embedding aktualisieren,
 * Stellen einsammeln, Matches neu berechnen und alles als {@link SearchRun} protokollieren.
 */
@Service
public class JobSearchService {

    private static final Logger log = LoggerFactory.getLogger(JobSearchService.class);

    private final ProfileRepository profileRepository;
    private final PreferencesRepository preferencesRepository;
    private final CvStructuredRepository cvRepository;
    private final JobIngestionService ingestionService;
    private final JobEmbeddingService embeddingService;
    private final SearchRunRepository searchRunRepository;
    private final ObjectMapper objectMapper;
    private final MatchRecomputer matchRecomputer;

    public JobSearchService(ProfileRepository profileRepository,
                            PreferencesRepository preferencesRepository,
                            CvStructuredRepository cvRepository,
                            JobIngestionService ingestionService,
                            JobEmbeddingService embeddingService,
                            SearchRunRepository searchRunRepository,
                            ObjectMapper objectMapper,
                            MatchRecomputer matchRecomputer) {
        this.profileRepository = profileRepository;
        this.preferencesRepository = preferencesRepository;
        this.cvRepository = cvRepository;
        this.ingestionService = ingestionService;
        this.embeddingService = embeddingService;
        this.searchRunRepository = searchRunRepository;
        this.objectMapper = objectMapper;
        this.matchRecomputer = matchRecomputer;
    }

    public SearchRun runForUser(UUID userId) {
        Optional<UserProfile> profile = profileRepository.findByUserId(userId);
        Optional<UserPreferences> prefs = preferencesRepository.findByUserId(userId);

        JobQuery query = buildQuery(profile.orElse(null), prefs.orElse(null));

        SearchRun run = new SearchRun(userId);
        run = searchRunRepository.save(run);

        try {
            // Profil-Embedding aktuell halten (für die semantische Suche).
            embeddingService.embedProfile(userId, profileText(profile.orElse(null),
                    prefs.orElse(null), cvRepository.findByUserId(userId).orElse(null)));

            JobIngestionService.IngestStats stats = ingestionService.ingest(query);
            int matches = matchRecomputer.recompute(userId);

            run.setSources(stats.sources().toArray(new String[0]));
            run.setStatus("DONE");
            run.setStats(objectMapper.writeValueAsString(Map.of(
                    "fetched", stats.fetched(),
                    "created", stats.created(),
                    "matches", matches)));
        } catch (Exception e) {
            log.error("Suchlauf für {} fehlgeschlagen", userId, e);
            run.setStatus("FAILED");
            safeStats(run, e.getMessage());
        } finally {
            run.setFinishedAt(Instant.now());
            run = searchRunRepository.save(run);
        }
        return run;
    }

    JobQuery buildQuery(UserProfile profile, UserPreferences prefs) {
        Set<String> keywords = new LinkedHashSet<>();
        if (prefs != null) {
            addAll(keywords, prefs.getDesiredTitles());
            addAll(keywords, prefs.getKeywords());
        }
        String location = profile != null ? profile.getLocation() : null;
        boolean remoteOnly = profile != null && "REMOTE".equalsIgnoreCase(profile.getRemotePref());
        Integer radius = location != null && !location.isBlank() ? 50 : null;
        return new JobQuery(new ArrayList<>(keywords), location, radius, remoteOnly, 50);
    }

    String profileText(UserProfile profile, UserPreferences prefs, CvStructured cv) {
        StringBuilder sb = new StringBuilder();
        if (profile != null) {
            append(sb, profile.getHeadline());
            append(sb, profile.getSummary());
            append(sb, profile.getLocation());
        }
        if (prefs != null) {
            append(sb, String.join(", ", nullSafe(prefs.getDesiredTitles())));
            append(sb, String.join(", ", nullSafe(prefs.getIndustries())));
            append(sb, String.join(", ", nullSafe(prefs.getKeywords())));
        }
        if (cv != null) {
            append(sb, cv.getSkills());
            append(sb, cv.getExperience());
        }
        return sb.toString().strip();
    }

    private void safeStats(SearchRun run, String message) {
        try {
            run.setStats(objectMapper.writeValueAsString(Map.of("error", message == null ? "" : message)));
        } catch (Exception ignored) {
            run.setStats("{}");
        }
    }

    private void append(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(value).append('\n');
        }
    }

    private void addAll(Set<String> set, String[] values) {
        if (values != null) {
            for (String v : values) {
                if (v != null && !v.isBlank()) {
                    set.add(v);
                }
            }
        }
    }

    private String[] nullSafe(String[] arr) {
        return arr == null ? new String[0] : arr;
    }
}
