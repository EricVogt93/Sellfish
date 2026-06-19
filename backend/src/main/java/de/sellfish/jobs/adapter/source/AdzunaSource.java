package de.sellfish.jobs.adapter.source;

import com.fasterxml.jackson.databind.JsonNode;
import de.sellfish.jobs.port.JobQuery;
import de.sellfish.jobs.port.JobSource;
import de.sellfish.jobs.port.RawJob;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Stellen aus der Adzuna-API. Benötigt {@code app_id} und {@code app_key} in der Quellen-Config.
 */
@Component
public class AdzunaSource implements JobSource {

    public static final String CODE = "ADZUNA";
    private static final String BASE_URL = "https://api.adzuna.com/v1/api";

    private static final Logger log = LoggerFactory.getLogger(AdzunaSource.class);

    private final RestClient client;

    public AdzunaSource(RestClient.Builder builder) {
        this.client = builder.baseUrl(BASE_URL).build();
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<RawJob> fetch(JobQuery query, Map<String, Object> config) {
        String appId = str(config, "app_id");
        String appKey = str(config, "app_key");
        String country = config.getOrDefault("country", "de").toString();
        if (appId == null || appKey == null) {
            log.warn("Adzuna without app_id/app_key configured - skipped");
            return List.of();
        }
        try {
            JsonNode response = client.get()
                    .uri(uri -> {
                        var b = uri.path("/jobs/{country}/search/1")
                                .queryParam("app_id", appId)
                                .queryParam("app_key", appKey)
                                .queryParam("results_per_page", Math.max(1, query.size()))
                                .queryParam("content-type", "application/json");
                        if (!query.keywordString().isBlank()) {
                            b.queryParam("what", query.keywordString());
                        }
                        if (query.location() != null && !query.location().isBlank()) {
                            b.queryParam("where", query.location());
                        }
                        return b.build(country);
                    })
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                return List.of();
            }
            List<RawJob> jobs = new ArrayList<>();
            for (JsonNode item : response.path("results")) {
                jobs.add(toRawJob(item));
            }
            return jobs;
        } catch (RestClientException e) {
            log.warn("Adzuna-Abruf failed: {}", e.getMessage());
            return List.of();
        }
    }

    private RawJob toRawJob(JsonNode item) {
        String salary = null;
        if (item.path("salary_min").isNumber()) {
            salary = item.path("salary_min").asText() + "–"
                    + item.path("salary_max").asText("");
        }
        return new RawJob(
                CODE,
                item.path("id").asText(null),
                item.path("title").asText("Stelle"),
                item.path("company").path("display_name").asText(null),
                item.path("location").path("display_name").asText(null),
                null,
                item.path("description").asText(null),
                item.path("redirect_url").asText(null),
                salary,
                parseDate(item.path("created").asText(null)),
                item.toString());
    }

    private Instant parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(date).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String str(Map<String, Object> config, String key) {
        Object v = config.get(key);
        return v == null ? null : v.toString();
    }
}
