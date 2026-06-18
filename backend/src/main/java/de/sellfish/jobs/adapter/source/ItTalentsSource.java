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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * IT-Talents — deutsches IT-Job-Board (keyless, JSON-API).
 */
@Component
public class ItTalentsSource implements JobSource {

    public static final String CODE = "ITTALENTS";
    private static final String BASE_URL = "https://www.it-talents.de/api";
    private static final Logger log = LoggerFactory.getLogger(ItTalentsSource.class);

    private final RestClient client;

    public ItTalentsSource(RestClient.Builder builder) {
        this.client = builder.baseUrl(BASE_URL)
                .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; SellfishBot/1.0)")
                .build();
    }

    @Override
    public String code() { return CODE; }

    @Override
    public List<RawJob> fetch(JobQuery query, Map<String, Object> config) {
        try {
            String q = query.keywordString().isBlank() ? "entwickler" : query.keywordString();
            JsonNode response = client.get()
                    .uri("/jobs?size=" + Math.min(100, query.size()) + "&q=" + urlEncode(q))
                    .retrieve().body(JsonNode.class);
            if (response == null) return List.of();
            List<RawJob> jobs = new ArrayList<>();
            JsonNode content = response.path("content");
            if (!content.isArray()) return List.of();
            for (JsonNode item : content) {
                jobs.add(toRawJob(item));
                if (jobs.size() >= query.size()) break;
            }
            return jobs;
        } catch (RestClientException e) {
            log.warn("IT-Talents-Abruf fehlgeschlagen: {}", e.getMessage());
            return List.of();
        }
    }

    private RawJob toRawJob(JsonNode item) {
        return new RawJob(CODE,
                JobSourceSupport.text(item, "id"),
                JobSourceSupport.text(item, "title"),
                JobSourceSupport.text(item.path("company"), "name"),
                city(item),
                item.path("remote").asBoolean(false) ? "REMOTE" : null,
                JobSourceSupport.text(item, "description"),
                jobUrl(item),
                null,
                parseDate(item),
                item.toString());
    }

    private String city(JsonNode item) {
        JsonNode loc = item.path("location");
        if (loc.isMissingNode()) return null;
        return JobSourceSupport.firstText(loc, "city", "name");
    }

    private String jobUrl(JsonNode item) {
        String id = JobSourceSupport.text(item, "id");
        String slug = JobSourceSupport.text(item, "slug");
        if (slug == null) slug = JobSourceSupport.text(item, "title");
        return "https://www.it-talents.de/job/" + slug + "-" + id;
    }

    private Instant parseDate(JsonNode item) {
        return JobSourceSupport.parseIso(JobSourceSupport.text(item, "publishedAt"));
    }

    private String urlEncode(String s) {
        try { return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8); }
        catch (Exception e) { return s; }
    }
}
