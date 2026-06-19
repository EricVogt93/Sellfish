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
 * CareerJet Job-Search-API (benötigt api_key, disabled by default).
 * Konfiguration: {"api_key": "..."}
 */
@Component
public class CareerJetSource implements JobSource {

    public static final String CODE = "CAREERJET";
    private static final String BASE_URL = "https://www.careerjet.com/api/search";
    private static final Logger log = LoggerFactory.getLogger(CareerJetSource.class);

    private final RestClient client;

    public CareerJetSource(RestClient.Builder builder) {
        this.client = builder.build();
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<RawJob> fetch(JobQuery query, Map<String, Object> config) {
        String apiKey = (String) config.get("api_key");
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("CareerJet without api_key configured - skipped");
            return List.of();
        }
        try {
            String q = query.keywordString().isBlank() ? "developer" : query.keywordString();
            String loc = query.location() != null ? query.location() : "";
            JsonNode response = client.get()
                    .uri(BASE_URL + "?locale_code=de_DE&keywords=" + urlEncode(q)
                            + "&location=" + urlEncode(loc) + "&pagesize=" + Math.min(99, query.size())
                            + "&affid=" + urlEncode(apiKey))
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) return List.of();
            List<RawJob> jobs = new ArrayList<>();
            for (JsonNode item : response.path("jobs")) {
                jobs.add(toRawJob(item));
                if (jobs.size() >= query.size()) break;
            }
            return jobs;
        } catch (RestClientException e) {
            log.warn("CareerJet-Abruf failed: {}", e.getMessage());
            return List.of();
        }
    }

    private RawJob toRawJob(JsonNode item) {
        String salary = JobSourceSupport.text(item, "salary");
        return new RawJob(
                CODE,
                JobSourceSupport.text(item, "url"),
                JobSourceSupport.text(item, "title"),
                JobSourceSupport.text(item, "company"),
                JobSourceSupport.text(item, "locations"),
                null,
                JobSourceSupport.text(item, "description"),
                JobSourceSupport.text(item, "url"),
                salary,
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
