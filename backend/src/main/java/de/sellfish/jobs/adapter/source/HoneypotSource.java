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
import java.util.Map;

/**
 * Honeypot — DE/EU Tech-Recruiting-Plattform mit öffentlicher JSON-API (keyless).
 */
@Component
public class HoneypotSource implements JobSource {

    public static final String CODE = "HONEYPOT";
    private static final String BASE_URL = "https://www.honeypot.io/api";
    private static final Logger log = LoggerFactory.getLogger(HoneypotSource.class);

    private final RestClient client;

    public HoneypotSource(RestClient.Builder builder) {
        this.client = builder.baseUrl(BASE_URL)
                .defaultHeader("User-Agent", "Mozilla/5.0")
                .build();
    }

    @Override
    public String code() { return CODE; }

    @Override
    public List<RawJob> fetch(JobQuery query, Map<String, Object> config) {
        try {
            JsonNode response = client.get()
                    .uri("/jobs/search?limit=" + Math.min(100, query.size())
                            + "&country[]=germany")
                    .retrieve().body(JsonNode.class);
            if (response == null || !response.has("results")) return List.of();
            List<RawJob> jobs = new ArrayList<>();
            for (JsonNode item : response.path("results")) {
                jobs.add(toRawJob(item));
                if (jobs.size() >= query.size()) break;
            }
            return jobs;
        } catch (RestClientException e) {
            log.warn("Honeypot-Abruf fehlgeschlagen: {}", e.getMessage());
            return List.of();
        }
    }

    private RawJob toRawJob(JsonNode item) {
        JsonNode company = item.path("company");
        return new RawJob(CODE,
                JobSourceSupport.text(item, "id"),
                JobSourceSupport.text(item, "title"),
                company.path("name").asText(null),
                location(item),
                item.path("remote").asBoolean(false) ? "REMOTE" : null,
                JobSourceSupport.text(item, "description"),
                "https://www.honeypot.io" + item.path("url").asText(""),
                salary(item),
                JobSourceSupport.parseIso(JobSourceSupport.text(item, "published_at")),
                item.toString());
    }

    private String location(JsonNode item) {
        JsonNode locs = item.path("locations");
        if (!locs.isArray() || locs.isEmpty()) return null;
        List<String> parts = new ArrayList<>();
        for (JsonNode l : locs) {
            String city = l.path("city").asText(null);
            if (city != null) parts.add(city);
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private String salary(JsonNode item) {
        JsonNode s = item.path("salary");
        if (s.isMissingNode()) return null;
        return s.path("display").asText(null);
    }
}
