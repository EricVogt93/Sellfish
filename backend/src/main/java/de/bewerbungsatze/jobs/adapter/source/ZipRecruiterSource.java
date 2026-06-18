package de.bewerbungsatze.jobs.adapter.source;
import de.bewerbungsatze.jobs.port.JobSource;
import de.bewerbungsatze.jobs.port.JobQuery;
import de.bewerbungsatze.jobs.port.RawJob;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ZipRecruiter Job-Search-API (benötigt api_key, disabled by default).
 * Konfiguration: {"api_key": "..."}
 */
@Component
public class ZipRecruiterSource implements JobSource {

    public static final String CODE = "ZIPRECRUITER";
    private static final String BASE_URL = "https://api.ziprecruiter.com/jobs/v1";
    private static final Logger log = LoggerFactory.getLogger(ZipRecruiterSource.class);

    private final RestClient client;

    public ZipRecruiterSource(RestClient.Builder builder) {
        this.client = builder.build();
    }

    @Override
    public String code() { return CODE; }

    @Override
    public List<RawJob> fetch(JobQuery query, Map<String, Object> config) {
        String apiKey = (String) config.get("api_key");
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("ZipRecruiter ohne api_key konfiguriert – übersprungen");
            return List.of();
        }
        try {
            String search = query.keywordString().isBlank() ? "developer" : query.keywordString();
            String location = query.location() != null ? query.location() : "";
            JsonNode response = client.get()
                    .uri(uri -> uri.path("")
                            .queryParam("search", search)
                            .queryParam("location", location)
                            .queryParam("jobs_per_page", Math.min(100, query.size()))
                            .queryParam("days_ago", 30)
                            .build())
                    .header("Authorization", "Basic " + Base64.getEncoder()
                            .encodeToString((apiKey + ":").getBytes()))
                    .accept(MediaType.APPLICATION_JSON)
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
            log.warn("ZipRecruiter-Abruf fehlgeschlagen: {}", e.getMessage());
            return List.of();
        }
    }

    private RawJob toRawJob(JsonNode item) {
        return new RawJob(CODE,
                JobSourceSupport.text(item, "id"),
                JobSourceSupport.text(item, "name"),
                JobSourceSupport.text(item.path("hiring_company"), "name"),
                JobSourceSupport.text(item, "location"),
                JobSourceSupport.text(item, "work_type"),
                JobSourceSupport.text(item, "snippet"),
                JobSourceSupport.text(item, "url"),
                JobSourceSupport.text(item, "salary_minimum"),
                null,
                item.toString());
    }
}
