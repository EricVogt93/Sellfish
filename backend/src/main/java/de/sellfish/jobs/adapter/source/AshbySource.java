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
 * Ashby Posting-API (ATS). Config {@code orgs}: kommagetrennte Job-Board-Namen.
 */
@Component
public class AshbySource implements JobSource {

    public static final String CODE = "ASHBY";
    private static final String BASE_URL = "https://api.ashbyhq.com/posting-api/job-board";
    private static final Logger log = LoggerFactory.getLogger(AshbySource.class);

    private final RestClient client;

    public AshbySource(RestClient.Builder builder) {
        this.client = builder.baseUrl(BASE_URL).build();
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<RawJob> fetch(JobQuery query, Map<String, Object> config) {
        Object orgs = config.get("orgs");
        if (orgs == null) {
            log.warn("Ashby without orgs configured - skipped");
            return List.of();
        }
        List<RawJob> jobs = new ArrayList<>();
        for (String org : orgs.toString().split(",")) {
            String name = org.strip();
            if (!name.isEmpty()) {
                jobs.addAll(fetchOrg(name, query));
            }
        }
        return jobs;
    }

    private List<RawJob> fetchOrg(String org, JobQuery query) {
        try {
            JsonNode response = client.get().uri("/{org}", org).retrieve().body(JsonNode.class);
            if (response == null) {
                return List.of();
            }
            List<RawJob> jobs = new ArrayList<>();
            for (JsonNode item : response.path("jobs")) {
                jobs.add(toRawJob(item, org));
                if (jobs.size() >= query.size()) {
                    break;
                }
            }
            return jobs;
        } catch (RestClientException e) {
            log.warn("Ashby-Org {} failed: {}", org, e.getMessage());
            return List.of();
        }
    }

    private RawJob toRawJob(JsonNode item, String org) {
        boolean remote = item.path("isRemote").asBoolean(false);
        return new RawJob(
                CODE,
                org + ":" + item.path("id").asText(""),
                JobSourceSupport.text(item, "title"),
                org,
                JobSourceSupport.text(item, "location"),
                remote ? "REMOTE" : null,
                JobSourceSupport.firstText(item, "descriptionPlain", "descriptionHtml"),
                JobSourceSupport.firstText(item, "jobUrl", "applyUrl"),
                null,
                JobSourceSupport.parseIso(JobSourceSupport.text(item, "publishedAt")),
                item.toString());
    }
}
