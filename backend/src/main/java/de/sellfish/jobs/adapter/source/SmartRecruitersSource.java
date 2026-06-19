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
 * SmartRecruiters Posting-API (ATS, öffentlich). Config {@code companies}: kommagetrennte Identifier.
 */
@Component
public class SmartRecruitersSource implements JobSource {

    public static final String CODE = "SMARTRECRUITERS";
    private static final String BASE_URL = "https://api.smartrecruiters.com/v1/companies";
    private static final Logger log = LoggerFactory.getLogger(SmartRecruitersSource.class);

    private final RestClient client;

    public SmartRecruitersSource(RestClient.Builder builder) {
        this.client = builder.baseUrl(BASE_URL).build();
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<RawJob> fetch(JobQuery query, Map<String, Object> config) {
        Object companies = config.get("companies");
        if (companies == null) {
            log.warn("SmartRecruiters without companies configured - skipped");
            return List.of();
        }
        List<RawJob> jobs = new ArrayList<>();
        for (String company : companies.toString().split(",")) {
            String handle = company.strip();
            if (!handle.isEmpty()) {
                jobs.addAll(fetchCompany(handle, query));
            }
        }
        return jobs;
    }

    private List<RawJob> fetchCompany(String company, JobQuery query) {
        try {
            JsonNode response = client.get()
                    .uri(uri -> uri.path("/{company}/postings")
                            .queryParam("limit", Math.min(Math.max(query.size(), 1), 100))
                            .build(company))
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                return List.of();
            }
            List<RawJob> jobs = new ArrayList<>();
            for (JsonNode item : response.path("content")) {
                jobs.add(toRawJob(item, company));
            }
            return jobs;
        } catch (RestClientException e) {
            log.warn("SmartRecruiters-Company {} failed: {}", company, e.getMessage());
            return List.of();
        }
    }

    private RawJob toRawJob(JsonNode item, String company) {
        JsonNode loc = item.path("location");
        String location = JobSourceSupport.firstText(loc, "city", "country");
        String id = item.path("id").asText("");
        return new RawJob(
                CODE,
                company + ":" + id,
                JobSourceSupport.text(item, "name"),
                company,
                location,
                loc.path("remote").asBoolean(false) ? "REMOTE" : null,
                null,
                "https://jobs.smartrecruiters.com/" + company + "/" + id,
                null,
                JobSourceSupport.parseIso(JobSourceSupport.text(item, "releasedDate")),
                item.toString());
    }
}
