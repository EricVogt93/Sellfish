package de.sellfish.jobs.adapter.source;
import de.sellfish.jobs.port.JobSource;
import de.sellfish.jobs.port.JobQuery;
import de.sellfish.jobs.port.RawJob;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Jooble-API (international, Aggregator). Benötigt {@code api_key} in der Config (POST-Endpoint).
 */
@Component
public class JoobleSource implements JobSource {

    public static final String CODE = "JOOBLE";
    private static final String BASE_URL = "https://jooble.org/api";
    private static final Logger log = LoggerFactory.getLogger(JoobleSource.class);

    private final RestClient.Builder builder;

    public JoobleSource(RestClient.Builder builder) {
        this.builder = builder;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<RawJob> fetch(JobQuery query, Map<String, Object> config) {
        Object apiKey = config.get("api_key");
        if (apiKey == null) {
            log.warn("Jooble without api_key configured - skipped");
            return List.of();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("keywords", query.keywordString());
        if (query.location() != null) {
            body.put("location", query.location());
        }
        try {
            RestClient client = builder.baseUrl(BASE_URL).build();
            JsonNode response = client.post()
                    .uri("/{key}", apiKey.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                return List.of();
            }
            List<RawJob> jobs = new ArrayList<>();
            for (JsonNode item : response.path("jobs")) {
                jobs.add(toRawJob(item));
                if (jobs.size() >= query.size()) {
                    break;
                }
            }
            return jobs;
        } catch (RestClientException e) {
            log.warn("Jooble-Abruf failed: {}", e.getMessage());
            return List.of();
        }
    }

    private RawJob toRawJob(JsonNode item) {
        return new RawJob(
                CODE,
                JobSourceSupport.text(item, "id"),
                JobSourceSupport.text(item, "title"),
                JobSourceSupport.text(item, "company"),
                JobSourceSupport.text(item, "location"),
                null,
                JobSourceSupport.text(item, "snippet"),
                JobSourceSupport.text(item, "link"),
                JobSourceSupport.text(item, "salary"),
                JobSourceSupport.parseIso(JobSourceSupport.text(item, "updated")),
                item.toString());
    }
}
