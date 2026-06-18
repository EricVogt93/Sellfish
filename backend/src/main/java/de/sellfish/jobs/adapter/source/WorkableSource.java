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
 * Workable ATS Board-API (keyless, pro Company ein Board).
 * Konfiguration: {"companies": ["company1", "company2"]}
 */
@Component
public class WorkableSource implements JobSource {

    public static final String CODE = "WORKABLE";
    private static final String BASE_URL = "https://www.workable.com/api";
    private static final Logger log = LoggerFactory.getLogger(WorkableSource.class);

    private final RestClient client;

    public WorkableSource(RestClient.Builder builder) {
        this.client = builder.baseUrl(BASE_URL).build();
    }

    @Override
    public String code() { return CODE; }

    @Override
    public List<RawJob> fetch(JobQuery query, Map<String, Object> config) {
        List<String> companies = companies(config);
        if (companies.isEmpty()) {
            log.warn("Workable ohne companies konfiguriert – übersprungen");
            return List.of();
        }
        List<RawJob> jobs = new ArrayList<>();
        for (String company : companies) {
            try {
                JsonNode response = client.get()
                        .uri("/accounts/{company}/jobs", company.trim())
                        .retrieve()
                        .body(JsonNode.class);
                if (response != null && response.has("jobs")) {
                    for (JsonNode item : response.get("jobs")) {
                        jobs.add(toRawJob(item, company));
                        if (jobs.size() >= query.size()) break;
                    }
                }
            } catch (RestClientException e) {
                log.warn("Workable-Company {} fehlgeschlagen: {}", company, e.getMessage());
            }
            if (jobs.size() >= query.size()) break;
        }
        return jobs;
    }

    @SuppressWarnings("unchecked")
    private List<String> companies(Map<String, Object> config) {
        Object c = config.get("companies");
        if (c instanceof List<?> l && !l.isEmpty()) return (List<String>) l;
        return List.of();
    }

    private RawJob toRawJob(JsonNode item, String company) {
        String location = JobSourceSupport.text(item, "city");
        String country = JobSourceSupport.text(item, "country");
        String loc = (location != null ? location : "") + (country != null ? ", " + country : "");
        return new RawJob(CODE,
                JobSourceSupport.text(item, "shortcode"),
                JobSourceSupport.text(item, "title"),
                JobSourceSupport.text(item, "company"),
                loc.isBlank() ? null : loc.trim(),
                JobSourceSupport.text(item, "workplace"),
                JobSourceSupport.text(item, "description"),
                JobSourceSupport.text(item, "url"),
                null,
                JobSourceSupport.parseIso(JobSourceSupport.text(item, "published_on")),
                item.toString());
    }
}
