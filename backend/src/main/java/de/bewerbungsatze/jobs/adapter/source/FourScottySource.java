package de.bewerbungsatze.jobs.adapter.source;
import de.bewerbungsatze.jobs.port.JobSource;
import de.bewerbungsatze.jobs.port.JobQuery;
import de.bewerbungsatze.jobs.port.RawJob;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 4Scotty — deutsches IT-Job-Board mit öffentlicher API (keyless).
 * Enthaltene Daten: Titel, Firma, Location, Skills, Gehalt (wenn angegeben).
 */
@Component
public class FourScottySource implements JobSource {

    public static final String CODE = "4SCOTTY";
    private static final String BASE_URL = "https://www.4scotty.com/api";
    private static final Logger log = LoggerFactory.getLogger(FourScottySource.class);

    private final RestClient client;

    public FourScottySource(RestClient.Builder builder) {
        this.client = builder.baseUrl(BASE_URL).build();
    }

    @Override
    public String code() { return CODE; }

    @Override
    public List<RawJob> fetch(JobQuery query, Map<String, Object> config) {
        try {
            String q = query.keywordString().isBlank() ? "entwickler" : query.keywordString();
            JsonNode response = client.get()
                    .uri("/v2/jobs?limit=" + Math.min(100, query.size())
                            + "&q=" + urlEncode(q))
                    .retrieve().body(JsonNode.class);
            if (response == null || !response.has("data")) return List.of();
            List<RawJob> jobs = new ArrayList<>();
            for (JsonNode item : response.path("data")) {
                jobs.add(toRawJob(item));
                if (jobs.size() >= query.size()) break;
            }
            return jobs;
        } catch (RestClientException e) {
            log.warn("4Scotty-Abruf fehlgeschlagen: {}", e.getMessage());
            return List.of();
        }
    }

    private RawJob toRawJob(JsonNode item) {
        JsonNode company = item.path("company");
        return new RawJob(CODE,
                JobSourceSupport.text(item, "id"),
                JobSourceSupport.text(item, "title"),
                company.path("name").asText(null),
                JobSourceSupport.text(item, "location"),
                item.path("remote").asBoolean(false) ? "REMOTE" : null,
                description(item),
                JobSourceSupport.text(item, "url"),
                salary(item),
                JobSourceSupport.parseIso(JobSourceSupport.text(item, "publishedAt")),
                item.toString());
    }

    private String description(JsonNode item) {
        var sb = new StringBuilder();
        for (JsonNode s : item.path("skills")) sb.append(s.asText()).append(", ");
        for (JsonNode t : item.path("tasks")) sb.append(t.asText()).append(". ");
        return sb.toString().trim();
    }

    private String salary(JsonNode item) {
        JsonNode s = item.path("salary");
        if (s.isMissingNode()) return null;
        String min = s.path("min").asText(null);
        String max = s.path("max").asText(null);
        String cur = s.path("currency").asText("EUR");
        if (min != null && max != null) return min + "–" + max + " " + cur;
        if (min != null) return "ab " + min + " " + cur;
        return null;
    }

    private String urlEncode(String s) {
        try { return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8); }
        catch (Exception e) { return s; }
    }
}
