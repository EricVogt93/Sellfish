package de.sellfish.jobs.adapter.source;

import com.fasterxml.jackson.databind.JsonNode;
import de.sellfish.jobs.port.JobQuery;
import de.sellfish.jobs.port.JobSource;
import de.sellfish.jobs.port.RawJob;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Reed.co.uk-API (UK). Benötigt {@code api_key} (HTTP-Basic, Key als Benutzername) in der Config.
 */
@Component
public class ReedSource implements JobSource {

    public static final String CODE = "REED";
    private static final String BASE_URL = "https://www.reed.co.uk/api/1.0";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.UK);
    private static final Logger log = LoggerFactory.getLogger(ReedSource.class);

    private final RestClient.Builder builder;

    public ReedSource(RestClient.Builder builder) {
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
            log.warn("Reed without api_key configured - skipped");
            return List.of();
        }
        String basic = Base64.getEncoder().encodeToString((apiKey + ":").getBytes(StandardCharsets.UTF_8));
        try {
            RestClient client = builder.baseUrl(BASE_URL).build();
            JsonNode response = client.get()
                    .uri(uri -> {
                        var b = uri.path("/search")
                                .queryParam("resultsToTake", Math.min(Math.max(query.size(), 1), 100));
                        if (!query.keywordString().isBlank()) {
                            b.queryParam("keywords", query.keywordString());
                        }
                        if (query.location() != null && !query.location().isBlank()) {
                            b.queryParam("locationName", query.location());
                        }
                        return b.build();
                    })
                    .header("Authorization", "Basic " + basic)
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
            log.warn("Reed-Abruf failed: {}", e.getMessage());
            return List.of();
        }
    }

    private RawJob toRawJob(JsonNode item) {
        String salary = null;
        if (item.path("minimumSalary").isNumber()) {
            salary = item.path("minimumSalary").asText() + "–"
                    + item.path("maximumSalary").asText("");
        }
        return new RawJob(
                CODE,
                JobSourceSupport.text(item, "jobId"),
                JobSourceSupport.text(item, "jobTitle"),
                JobSourceSupport.text(item, "employerName"),
                JobSourceSupport.text(item, "locationName"),
                null,
                JobSourceSupport.text(item, "jobDescription"),
                JobSourceSupport.text(item, "jobUrl"),
                salary,
                parseDate(JobSourceSupport.text(item, "date")),
                item.toString());
    }

    private java.time.Instant parseDate(String date) {
        if (date == null) {
            return null;
        }
        try {
            return LocalDate.parse(date, DATE).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            return null;
        }
    }
}
