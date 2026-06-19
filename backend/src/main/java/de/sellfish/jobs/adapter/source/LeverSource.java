package de.sellfish.jobs.adapter.source;

import com.fasterxml.jackson.databind.JsonNode;
import de.sellfish.jobs.port.JobQuery;
import de.sellfish.jobs.port.JobSource;
import de.sellfish.jobs.port.RawJob;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Lever-Job-Postings (ATS). Config {@code companies}: kommagetrennte Lever-Handles.
 */
@Component
public class LeverSource implements JobSource {

    public static final String CODE = "LEVER";
    private static final String BASE_URL = "https://api.lever.co/v0/postings";
    private static final Logger log = LoggerFactory.getLogger(LeverSource.class);

    private final RestClient client;

    public LeverSource(RestClient.Builder builder) {
        this.client = builder.baseUrl(BASE_URL).build();
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<RawJob> fetch(JobQuery query, Map<String, Object> config) {
        Object companies = config.get("companies");
        if (companies == null) {
            log.warn("Lever without companies configured - skipped");
            return List.of();
        }
        List<RawJob> jobs = new ArrayList<>();
        for (String company : companies.toString().split(",")) {
            String handle = company.strip();
            if (handle.isEmpty()) {
                continue;
            }
            jobs.addAll(fetchCompany(handle, query));
        }
        return jobs;
    }

    private List<RawJob> fetchCompany(String company, JobQuery query) {
        try {
            JsonNode response = client.get()
                    .uri(uri ->
                            uri.path("/{company}").queryParam("mode", "json").build(company))
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.isArray()) {
                return List.of();
            }
            List<RawJob> jobs = new ArrayList<>();
            for (JsonNode item : response) {
                jobs.add(toRawJob(item, company));
                if (jobs.size() >= query.size()) {
                    break;
                }
            }
            return jobs;
        } catch (RestClientException e) {
            log.warn("Lever-Company {} failed: {}", company, e.getMessage());
            return List.of();
        }
    }

    private RawJob toRawJob(JsonNode item, String company) {
        JsonNode categories = item.path("categories");
        Instant created = item.path("createdAt").isNumber()
                ? Instant.ofEpochMilli(item.path("createdAt").asLong())
                : null;
        return new RawJob(
                CODE,
                company + ":" + item.path("id").asText(""),
                JobSourceSupport.firstText(item, "text"),
                company,
                categories.path("location").asText(null),
                "remote".equalsIgnoreCase(categories.path("commitment").asText("")) ? "REMOTE" : null,
                JobSourceSupport.firstText(item, "descriptionPlain", "description"),
                JobSourceSupport.text(item, "hostedUrl"),
                null,
                created,
                item.toString());
    }
}
