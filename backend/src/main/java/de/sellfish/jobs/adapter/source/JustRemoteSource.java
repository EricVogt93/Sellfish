package de.sellfish.jobs.adapter.source;
import de.sellfish.jobs.port.JobSource;
import de.sellfish.jobs.port.JobQuery;
import de.sellfish.jobs.port.RawJob;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * JustRemote — Remote-Jobs (keyless, API).
 */
@Component
public class JustRemoteSource implements JobSource {

    public static final String CODE = "JUSTRMOTE";
    private static final String BASE_URL = "https://justremote.co/api";
    private static final Logger log = LoggerFactory.getLogger(JustRemoteSource.class);

    private final RestClient client;

    public JustRemoteSource(RestClient.Builder builder) {
        this.client = builder.baseUrl(BASE_URL)
                .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; SellfishBot/1.0)")
                .build();
    }

    @Override
    public String code() { return CODE; }

    @Override
    public List<RawJob> fetch(JobQuery query, Map<String, Object> config) {
        try {
            JsonNode response = client.get()
                    .uri("/jobs?limit=" + Math.min(100, query.size()))
                    .retrieve().body(JsonNode.class);
            if (response == null || !response.has("jobs")) return List.of();
            List<RawJob> jobs = new ArrayList<>();
            for (JsonNode item : response.path("jobs")) {
                RawJob job = toRawJob(item);
                if (matches(job, query)) jobs.add(job);
                if (jobs.size() >= query.size()) break;
            }
            return jobs;
        } catch (RestClientException e) {
            log.warn("JustRemote-Abruf failed: {}", e.getMessage());
            return List.of();
        }
    }

    private boolean matches(RawJob job, JobQuery query) {
        if (query.keywords() == null || query.keywords().isEmpty()) return true;
        String haystack = (job.title() + " " + nz(job.company()) + " " + nz(job.description()))
                .toLowerCase(Locale.ROOT);
        return query.keywords().stream().anyMatch(k -> haystack.contains(k.toLowerCase(Locale.ROOT)));
    }

    private RawJob toRawJob(JsonNode item) {
        return new RawJob(CODE,
                JobSourceSupport.text(item, "id"),
                JobSourceSupport.text(item, "title"),
                JobSourceSupport.text(item, "company"),
                JobSourceSupport.text(item, "location"),
                "REMOTE",
                JobSourceSupport.text(item, "description"),
                JobSourceSupport.text(item, "url"),
                null,
                JobSourceSupport.parseIso(JobSourceSupport.text(item, "postedAt")),
                item.toString());
    }

    private String nz(String s) { return s == null ? "" : s; }
}
