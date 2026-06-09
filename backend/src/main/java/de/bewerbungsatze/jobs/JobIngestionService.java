package de.bewerbungsatze.jobs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bewerbungsatze.jobs.source.Fingerprints;
import de.bewerbungsatze.jobs.source.JobQuery;
import de.bewerbungsatze.jobs.source.JobSource;
import de.bewerbungsatze.jobs.source.RawJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Holt Stellen aus allen aktiven Quellen, dedupliziert sie und persistiert neue Jobs inkl. Embeddings.
 */
@Service
public class JobIngestionService {

    private static final Logger log = LoggerFactory.getLogger(JobIngestionService.class);

    private final List<JobSource> sources;
    private final JobSourceConfigRepository sourceConfigRepository;
    private final JobRepository jobRepository;
    private final JobEmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    public JobIngestionService(List<JobSource> sources,
                               JobSourceConfigRepository sourceConfigRepository,
                               JobRepository jobRepository,
                               JobEmbeddingService embeddingService,
                               ObjectMapper objectMapper) {
        this.sources = sources;
        this.sourceConfigRepository = sourceConfigRepository;
        this.jobRepository = jobRepository;
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
    }

    public IngestStats ingest(JobQuery query) {
        var enabled = sourceConfigRepository.findByEnabledTrue().stream()
                .collect(java.util.stream.Collectors.toMap(JobSourceConfig::getCode, c -> c));

        int fetched = 0;
        int created = 0;
        List<String> usedSources = new ArrayList<>();

        for (JobSource source : sources) {
            JobSourceConfig cfg = enabled.get(source.code());
            if (cfg == null) {
                continue;
            }
            usedSources.add(source.code());
            Map<String, Object> config = parseConfig(cfg.getConfig());
            List<RawJob> rawJobs;
            try {
                rawJobs = source.fetch(query, config);
            } catch (RuntimeException e) {
                log.warn("Quelle {} fehlgeschlagen: {}", source.code(), e.getMessage());
                continue;
            }
            fetched += rawJobs.size();
            for (RawJob raw : rawJobs) {
                if (persist(raw)) {
                    created++;
                }
            }
        }
        return new IngestStats(fetched, created, usedSources);
    }

    private boolean persist(RawJob raw) {
        String fingerprint = Fingerprints.of(raw);
        if (jobRepository.existsByFingerprint(fingerprint)) {
            return false;
        }
        Job job = new Job(raw.sourceCode(), fingerprint, raw.title());
        job.setExternalRef(raw.externalRef());
        job.setCompany(raw.company());
        job.setLocation(raw.location());
        job.setRemote(raw.remote());
        job.setDescription(raw.description());
        job.setUrl(raw.url());
        job.setSalaryRaw(raw.salaryRaw());
        job.setPostedAt(raw.postedAt());
        job.setRaw(raw.raw());
        Job saved;
        try {
            saved = jobRepository.save(job);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Paralleler Lauf hat denselben Fingerprint bereits angelegt.
            return false;
        }
        embeddingService.embedJob(saved);
        return true;
    }

    private Map<String, Object> parseConfig(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }

    public record IngestStats(int fetched, int created, List<String> sources) {
    }
}
