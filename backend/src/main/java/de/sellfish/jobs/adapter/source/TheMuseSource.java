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
 * The-Muse-Public-Jobs-API (kostenlos, optionaler {@code api_key} in der Config, international).
 */
@Component
public class TheMuseSource implements JobSource {

    public static final String CODE = "THEMUSE";
    private static final String BASE_URL = "https://www.themuse.com/api/public";
    private static final Logger log = LoggerFactory.getLogger(TheMuseSource.class);

    private final RestClient client;

    public TheMuseSource(RestClient.Builder builder) {
        this.client = builder.baseUrl(BASE_URL).build();
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<RawJob> fetch(JobQuery query, Map<String, Object> config) {
        Object apiKey = config.get("api_key");
        try {
            JsonNode response = client.get()
                    .uri(uri -> {
                        var b = uri.path("/jobs").queryParam("page", 0);
                        if (query.location() != null && !query.location().isBlank()) {
                            b.queryParam("location", query.location());
                        }
                        if (apiKey != null) {
                            b.queryParam("api_key", apiKey.toString());
                        }
                        return b.build();
                    })
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                return List.of();
            }
            List<RawJob> jobs = new ArrayList<>();
            for (JsonNode item : response.path("results")) {
                jobs.add(toRawJob(item));
                if (jobs.size() >= query.size()) {
                    break;
                }
            }
            return jobs;
        } catch (RestClientException e) {
            log.warn("TheMuse-Abruf failed: {}", e.getMessage());
            return List.of();
        }
    }

    private RawJob toRawJob(JsonNode item) {
        String location = null;
        JsonNode locations = item.path("locations");
        if (locations.isArray() && !locations.isEmpty()) {
            location = locations.get(0).path("name").asText(null);
        }
        return new RawJob(
                CODE,
                JobSourceSupport.text(item, "id"),
                JobSourceSupport.firstText(item, "name"),
                item.path("company").path("name").asText(null),
                location,
                null,
                JobSourceSupport.text(item, "contents"),
                item.path("refs").path("landing_page").asText(null),
                null,
                JobSourceSupport.parseIso(JobSourceSupport.text(item, "publication_date")),
                item.toString());
    }
}
