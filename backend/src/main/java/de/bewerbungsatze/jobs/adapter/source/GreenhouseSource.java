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
import java.util.Map;

/**
 * Greenhouse-Job-Boards (ATS). Config {@code boards}: kommagetrennte Board-Tokens (z. B. {@code stripe,airbnb}).
 */
@Component
public class GreenhouseSource implements JobSource {

    public static final String CODE = "GREENHOUSE";
    private static final String BASE_URL = "https://boards-api.greenhouse.io/v1/boards";
    private static final Logger log = LoggerFactory.getLogger(GreenhouseSource.class);

    private final RestClient client;

    public GreenhouseSource(RestClient.Builder builder) {
        this.client = builder.baseUrl(BASE_URL).build();
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<RawJob> fetch(JobQuery query, Map<String, Object> config) {
        Object boards = config.get("boards");
        if (boards == null) {
            log.warn("Greenhouse ohne boards konfiguriert – übersprungen");
            return List.of();
        }
        List<RawJob> jobs = new ArrayList<>();
        for (String board : boards.toString().split(",")) {
            String token = board.strip();
            if (token.isEmpty()) {
                continue;
            }
            jobs.addAll(fetchBoard(token, query));
        }
        return jobs;
    }

    private List<RawJob> fetchBoard(String board, JobQuery query) {
        try {
            JsonNode response = client.get()
                    .uri(uri -> uri.path("/{board}/jobs").queryParam("content", "true").build(board))
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                return List.of();
            }
            List<RawJob> jobs = new ArrayList<>();
            for (JsonNode item : response.path("jobs")) {
                jobs.add(toRawJob(item, board));
                if (jobs.size() >= query.size()) {
                    break;
                }
            }
            return jobs;
        } catch (RestClientException e) {
            log.warn("Greenhouse-Board {} fehlgeschlagen: {}", board, e.getMessage());
            return List.of();
        }
    }

    private RawJob toRawJob(JsonNode item, String board) {
        return new RawJob(
                CODE,
                board + ":" + item.path("id").asText(""),
                JobSourceSupport.text(item, "title"),
                board,
                item.path("location").path("name").asText(null),
                null,
                JobSourceSupport.text(item, "content"),
                JobSourceSupport.text(item, "absolute_url"),
                null,
                JobSourceSupport.parseIso(JobSourceSupport.text(item, "updated_at")),
                item.toString());
    }
}
