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
 * Jobicy Remote-Jobs-API v2 (kostenlos, without Key, international/remote).
 */
@Component
public class JobicySource implements JobSource {

    public static final String CODE = "JOBICY";
    private static final String BASE_URL = "https://jobicy.com/api/v2";
    private static final Logger log = LoggerFactory.getLogger(JobicySource.class);

    private final RestClient client;

    public JobicySource(RestClient.Builder builder) {
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
                    .uri(uri -> {
                        var b = uri.path("/remote-jobs").queryParam("count", Math.min(Math.max(query.size(), 1), 50));
                        if (!query.keywordString().isBlank()) {
                            b.queryParam("tag", query.keywordString());
                        }
                        return b.build();
                    })
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                return List.of();
            }
            List<RawJob> jobs = new ArrayList<>();
            for (JsonNode item : response.path("jobs")) {
                jobs.add(toRawJob(item));
            }
            return jobs;
        } catch (RestClientException e) {
            log.warn("Jobicy-Abruf failed: {}", e.getMessage());
            return List.of();
        }
    }

    private RawJob toRawJob(JsonNode item) {
        return new RawJob(
                CODE,
                JobSourceSupport.text(item, "id"),
                JobSourceSupport.firstText(item, "jobTitle", "title"),
                JobSourceSupport.text(item, "companyName"),
                JobSourceSupport.text(item, "jobGeo"),
                "REMOTE",
                JobSourceSupport.firstText(item, "jobDescription", "jobExcerpt"),
                JobSourceSupport.text(item, "url"),
                JobSourceSupport.text(item, "annualSalaryMax"),
                JobSourceSupport.parseIso(JobSourceSupport.text(item, "pubDate")),
                item.toString());
    }
}
