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
 * Findwork.dev-API (international, Tech-Jobs). Benötigt {@code api_key} (Token-Auth) in der Config.
 */
@Component
public class FindworkSource implements JobSource {

    public static final String CODE = "FINDWORK";
    private static final String BASE_URL = "https://findwork.dev/api";
    private static final Logger log = LoggerFactory.getLogger(FindworkSource.class);

    private final RestClient.Builder builder;

    public FindworkSource(RestClient.Builder builder) {
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
            log.warn("Findwork without api_key configured - skipped");
            return List.of();
        }
        try {
            RestClient client = builder.baseUrl(BASE_URL).build();
            JsonNode response = client.get()
                    .uri(uri -> {
                        var b = uri.path("/jobs/");
                        if (!query.keywordString().isBlank()) {
                            b.queryParam("search", query.keywordString());
                        }
                        if (query.location() != null && !query.location().isBlank()) {
                            b.queryParam("location", query.location());
                        }
                        return b.build();
                    })
                    .header("Authorization", "Token " + apiKey)
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
            log.warn("Findwork-Abruf failed: {}", e.getMessage());
            return List.of();
        }
    }

    private RawJob toRawJob(JsonNode item) {
        boolean remote = item.path("remote").asBoolean(false);
        return new RawJob(
                CODE,
                JobSourceSupport.text(item, "id"),
                JobSourceSupport.firstText(item, "role", "title"),
                JobSourceSupport.text(item, "company_name"),
                JobSourceSupport.text(item, "location"),
                remote ? "REMOTE" : null,
                JobSourceSupport.text(item, "text"),
                JobSourceSupport.text(item, "url"),
                null,
                JobSourceSupport.parseIso(JobSourceSupport.text(item, "date_posted")),
                item.toString());
    }
}
