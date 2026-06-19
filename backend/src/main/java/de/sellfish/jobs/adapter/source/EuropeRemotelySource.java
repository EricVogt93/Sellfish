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
 * EuropeRemotely — EU-Zeitzone Remote-Jobs (keyless, curated JSON-Feed).
 */
@Component
public class EuropeRemotelySource implements JobSource {

    public static final String CODE = "EURREMOTE";
    private static final String BASE_URL = "https://europeremotely.com";
    private static final Logger log = LoggerFactory.getLogger(EuropeRemotelySource.class);

    private final RestClient client;

    public EuropeRemotelySource(RestClient.Builder builder) {
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
                    .uri("/jobs.json").retrieve().body(JsonNode.class);
            if (response == null || !response.isArray()) return List.of();
            List<RawJob> jobs = new ArrayList<>();
            for (JsonNode item : response) {
                RawJob job = toRawJob(item);
                if (matches(job, query)) jobs.add(job);
                if (jobs.size() >= query.size()) break;
            }
            return jobs;
        } catch (RestClientException e) {
            log.warn("EuropeRemotely-Abruf failed: {}", e.getMessage());
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
                JobSourceSupport.text(item, "position"),
                JobSourceSupport.text(item, "company"),
                JobSourceSupport.text(item, "location"),
                "REMOTE",
                JobSourceSupport.text(item, "description"),
                JobSourceSupport.text(item, "url"),
                JobSourceSupport.text(item, "salary"),
                JobSourceSupport.parseIso(JobSourceSupport.text(item, "date")),
                item.toString());
    }

    private String nz(String s) { return s == null ? "" : s; }
}
