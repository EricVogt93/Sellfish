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
 * Recruitee-Offers-API (ATS, per Firmen-Subdomain). Config {@code companies}: kommagetrennte Handles.
 */
@Component
public class RecruiteeSource implements JobSource {

    public static final String CODE = "RECRUITEE";
    private static final Logger log = LoggerFactory.getLogger(RecruiteeSource.class);

    private final RestClient.Builder builder;

    public RecruiteeSource(RestClient.Builder builder) {
        this.builder = builder;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<RawJob> fetch(JobQuery query, Map<String, Object> config) {
        Object companies = config.get("companies");
        if (companies == null) {
            log.warn("Recruitee ohne companies konfiguriert – übersprungen");
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
            RestClient client = builder.baseUrl("https://" + company + ".recruitee.com").build();
            JsonNode response = client.get().uri("/api/offers/").retrieve().body(JsonNode.class);
            if (response == null) {
                return List.of();
            }
            List<RawJob> jobs = new ArrayList<>();
            for (JsonNode item : response.path("offers")) {
                jobs.add(toRawJob(item, company));
                if (jobs.size() >= query.size()) {
                    break;
                }
            }
            return jobs;
        } catch (RestClientException e) {
            log.warn("Recruitee-Company {} fehlgeschlagen: {}", company, e.getMessage());
            return List.of();
        }
    }

    private RawJob toRawJob(JsonNode item, String company) {
        String location = JobSourceSupport.firstText(item, "location", "city");
        return new RawJob(
                CODE,
                company + ":" + item.path("id").asText(""),
                JobSourceSupport.text(item, "title"),
                company,
                location,
                null,
                JobSourceSupport.text(item, "description"),
                JobSourceSupport.firstText(item, "careers_url", "careers_apply_url"),
                null,
                JobSourceSupport.parseIso(JobSourceSupport.text(item, "published_at")),
                item.toString());
    }
}
