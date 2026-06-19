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
 * Remotive Remote-Jobs-API (kostenlos, without Key, international/remote).
 */
@Component
public class RemotiveSource implements JobSource {

    public static final String CODE = "REMOTIVE";
    private static final String BASE_URL = "https://remotive.com/api";
    private static final Logger log = LoggerFactory.getLogger(RemotiveSource.class);

    private final RestClient client;

    public RemotiveSource(RestClient.Builder builder) {
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
                        var b = uri.path("/remote-jobs").queryParam("limit", Math.max(1, query.size()));
                        if (!query.keywordString().isBlank()) {
                            b.queryParam("search", query.keywordString());
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
            log.warn("Remotive-Abruf failed: {}", e.getMessage());
            return List.of();
        }
    }

    private RawJob toRawJob(JsonNode item) {
        return new RawJob(
                CODE,
                JobSourceSupport.text(item, "id"),
                JobSourceSupport.text(item, "title"),
                JobSourceSupport.text(item, "company_name"),
                JobSourceSupport.text(item, "candidate_required_location"),
                "REMOTE",
                JobSourceSupport.text(item, "description"),
                JobSourceSupport.text(item, "url"),
                JobSourceSupport.text(item, "salary"),
                JobSourceSupport.parseIso(JobSourceSupport.text(item, "publication_date")),
                item.toString());
    }
}
