package de.sellfish.jobs.adapter.source;

import com.fasterxml.jackson.databind.JsonNode;
import de.sellfish.jobs.port.JobQuery;
import de.sellfish.jobs.port.JobSource;
import de.sellfish.jobs.port.RawJob;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Himalayas Remote-Jobs-API (kostenlos, without Key, international/remote). Filterung clientseitig.
 */
@Component
public class HimalayasSource implements JobSource {

    public static final String CODE = "HIMALAYAS";
    private static final String BASE_URL = "https://himalayas.app";
    private static final Logger log = LoggerFactory.getLogger(HimalayasSource.class);

    private final RestClient client;

    public HimalayasSource(RestClient.Builder builder) {
        this.client = builder.baseUrl(BASE_URL).build();
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<RawJob> fetch(JobQuery query, Map<String, Object> config) {
        try {
            JsonNode response = client.get()
                    .uri(uri -> uri.path("/jobs/api")
                            .queryParam("limit", Math.min(Math.max(query.size(), 1), 50))
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                return List.of();
            }
            List<RawJob> jobs = new ArrayList<>();
            for (JsonNode item : response.path("jobs")) {
                RawJob job = toRawJob(item);
                if (matches(job, query)) {
                    jobs.add(job);
                }
            }
            return jobs;
        } catch (RestClientException e) {
            log.warn("Himalayas-Abruf failed: {}", e.getMessage());
            return List.of();
        }
    }

    private boolean matches(RawJob job, JobQuery query) {
        if (query.keywords() == null || query.keywords().isEmpty()) {
            return true;
        }
        String haystack =
                (job.title() + " " + (job.description() == null ? "" : job.description())).toLowerCase(Locale.ROOT);
        return query.keywords().stream().anyMatch(k -> haystack.contains(k.toLowerCase(Locale.ROOT)));
    }

    private RawJob toRawJob(JsonNode item) {
        String location = null;
        JsonNode restrictions = item.path("locationRestrictions");
        if (restrictions.isArray() && !restrictions.isEmpty()) {
            location = restrictions.get(0).asText(null);
        }
        return new RawJob(
                CODE,
                JobSourceSupport.firstText(item, "guid", "title"),
                JobSourceSupport.text(item, "title"),
                JobSourceSupport.text(item, "companyName"),
                location,
                "REMOTE",
                JobSourceSupport.firstText(item, "description", "excerpt"),
                JobSourceSupport.firstText(item, "applicationLink", "guid"),
                null,
                JobSourceSupport.parseEpochSeconds(item, "pubDate"),
                item.toString());
    }
}
