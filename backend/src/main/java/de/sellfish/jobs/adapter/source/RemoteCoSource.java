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
 * Remote.co — kuratierte Remote-Jobs mit JSON-API (keyless).
 */
@Component
public class RemoteCoSource implements JobSource {

    public static final String CODE = "REMOTECO";
    private static final String BASE_URL = "https://remote.co";
    private static final Logger log = LoggerFactory.getLogger(RemoteCoSource.class);

    private final RestClient client;

    public RemoteCoSource(RestClient.Builder builder) {
        this.client = builder.baseUrl(BASE_URL)
                .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; SellfishBot/1.0)")
                .build();
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<RawJob> fetch(JobQuery query, Map<String, Object> config) {
        try {
            JsonNode response = client.get()
                    .uri("/remote-jobs/search?search="
                            + urlEncode(query.keywordString().isBlank() ? "developer" : query.keywordString()))
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.isArray()) return List.of();
            List<RawJob> jobs = new ArrayList<>();
            for (JsonNode item : response) {
                JsonNode card = item.path("card");
                if (card.isMissingNode()) continue;
                jobs.add(toRawJob(card));
                if (jobs.size() >= query.size()) break;
            }
            return jobs;
        } catch (RestClientException e) {
            log.warn("Remote.co-Abruf failed: {}", e.getMessage());
            return List.of();
        }
    }

    private RawJob toRawJob(JsonNode item) {
        return new RawJob(
                CODE,
                JobSourceSupport.text(item, "id"),
                JobSourceSupport.text(item, "job_title"),
                JobSourceSupport.text(item, "company_name"),
                JobSourceSupport.text(item, "location"),
                "REMOTE",
                JobSourceSupport.text(item, "description"),
                JobSourceSupport.text(item, "url"),
                null,
                JobSourceSupport.parseIso(JobSourceSupport.text(item, "date")),
                item.toString());
    }

    private String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }
}
