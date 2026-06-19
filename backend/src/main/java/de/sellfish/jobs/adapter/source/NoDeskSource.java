package de.sellfish.jobs.adapter.source;

import com.fasterxml.jackson.databind.JsonNode;
import de.sellfish.jobs.port.JobQuery;
import de.sellfish.jobs.port.JobSource;
import de.sellfish.jobs.port.RawJob;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * NoDesk (remote jobs, keyless, curated).
 */
@Component
public class NoDeskSource implements JobSource {

    public static final String CODE = "NODESK";
    private static final String BASE_URL = "https://nodesk.co";
    private static final Logger log = LoggerFactory.getLogger(NoDeskSource.class);

    private final RestClient client;

    public NoDeskSource(RestClient.Builder builder) {
        this.client = builder.baseUrl(BASE_URL)
                .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; SellfishBot/1.0)")
                .build();
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<RawJob> fetch(JobQuery query, Map<String, Object> config) {
        try {
            JsonNode response = client.get().uri("/remote-jobs.json").retrieve().body(JsonNode.class);
            if (response == null || !response.isArray()) return List.of();
            List<RawJob> jobs = new ArrayList<>();
            for (JsonNode item : response) {
                RawJob job = toRawJob(item);
                if (matches(job, query)) jobs.add(job);
                if (jobs.size() >= query.size()) break;
            }
            return jobs;
        } catch (RestClientException e) {
            log.warn("NoDesk-Abruf failed: {}", e.getMessage());
            return List.of();
        }
    }

    private boolean matches(RawJob job, JobQuery query) {
        if (query.keywords() == null || query.keywords().isEmpty()) return true;
        String haystack = (job.title() + " " + nz(job.company()) + " " + nz(job.description()))
                .toLowerCase(java.util.Locale.ROOT);
        return query.keywords().stream().anyMatch(k -> haystack.contains(k.toLowerCase(java.util.Locale.ROOT)));
    }

    private RawJob toRawJob(JsonNode item) {
        return new RawJob(
                CODE,
                JobSourceSupport.text(item, "id"),
                JobSourceSupport.text(item, "title"),
                JobSourceSupport.text(item, "company"),
                JobSourceSupport.text(item, "location"),
                item.path("remote").asBoolean(true) ? "REMOTE" : null,
                JobSourceSupport.text(item, "description"),
                JobSourceSupport.text(item, "url"),
                null,
                JobSourceSupport.parseIso(JobSourceSupport.text(item, "date")),
                item.toString());
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
}
