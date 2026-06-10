package de.bewerbungsatze.jobs.adapter.source;
import de.bewerbungsatze.jobs.port.JobSource;
import de.bewerbungsatze.jobs.port.JobQuery;
import de.bewerbungsatze.jobs.port.RawJob;

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
 * USAJOBS-API (US-Behörden). Benötigt {@code api_key} und {@code email} (User-Agent) in der Config.
 */
@Component
public class UsaJobsSource implements JobSource {

    public static final String CODE = "USAJOBS";
    private static final String BASE_URL = "https://data.usajobs.gov/api";
    private static final Logger log = LoggerFactory.getLogger(UsaJobsSource.class);

    private final RestClient.Builder builder;

    public UsaJobsSource(RestClient.Builder builder) {
        this.builder = builder;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<RawJob> fetch(JobQuery query, Map<String, Object> config) {
        Object apiKey = config.get("api_key");
        Object email = config.get("email");
        if (apiKey == null || email == null) {
            log.warn("USAJOBS ohne api_key/email konfiguriert – übersprungen");
            return List.of();
        }
        try {
            RestClient client = builder
                    .baseUrl(BASE_URL)
                    .defaultHeader("Host", "data.usajobs.gov")
                    .defaultHeader("User-Agent", email.toString())
                    .defaultHeader("Authorization-Key", apiKey.toString())
                    .build();
            JsonNode response = client.get()
                    .uri(uri -> {
                        var b = uri.path("/search")
                                .queryParam("ResultsPerPage", Math.min(Math.max(query.size(), 1), 50));
                        if (!query.keywordString().isBlank()) {
                            b.queryParam("Keyword", query.keywordString());
                        }
                        if (query.location() != null && !query.location().isBlank()) {
                            b.queryParam("LocationName", query.location());
                        }
                        return b.build();
                    })
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                return List.of();
            }
            List<RawJob> jobs = new ArrayList<>();
            for (JsonNode item : response.path("SearchResult").path("SearchResultItems")) {
                jobs.add(toRawJob(item.path("MatchedObjectDescriptor")));
            }
            return jobs;
        } catch (RestClientException e) {
            log.warn("USAJOBS-Abruf fehlgeschlagen: {}", e.getMessage());
            return List.of();
        }
    }

    private RawJob toRawJob(JsonNode d) {
        JsonNode remuneration = d.path("PositionRemuneration");
        String salary = remuneration.isArray() && !remuneration.isEmpty()
                ? remuneration.get(0).path("MinimumRange").asText(null) : null;
        return new RawJob(
                CODE,
                JobSourceSupport.firstText(d, "PositionID"),
                JobSourceSupport.text(d, "PositionTitle"),
                d.path("OrganizationName").asText(null),
                d.path("PositionLocationDisplay").asText(null),
                null,
                d.path("UserArea").path("Details").path("JobSummary").asText(null),
                d.path("PositionURI").asText(null),
                salary,
                JobSourceSupport.parseIso(JobSourceSupport.text(d, "PublicationStartDate")),
                d.toString());
    }
}
