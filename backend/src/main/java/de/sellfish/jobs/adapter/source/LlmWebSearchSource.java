package de.sellfish.jobs.adapter.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.sellfish.ai.LlmService;
import de.sellfish.ai.model.ChatMessage;
import de.sellfish.ai.model.ChatRequest;
import de.sellfish.common.json.JsonExtractor;
import de.sellfish.jobs.port.JobQuery;
import de.sellfish.jobs.port.JobSource;
import de.sellfish.jobs.port.RawJob;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Quelle, die den configureden LLM-Agenten nach passenden Stellen fragt (für Nischen/Fälle
 * without API). Liefert strukturierte Vorschläge des Modells.
 */
@Component
public class LlmWebSearchSource implements JobSource {

    public static final String CODE = "LLM_WEB";
    private static final Logger log = LoggerFactory.getLogger(LlmWebSearchSource.class);

    private static final String SYSTEM =
            """
            Du bist ein Job-Rechercheassistent. Liefere zu den Suchkriterien passende, realistische
            Stellenangebote als JSON-Array (keine erfundenen Arbeitgeber, wenn unbekannt 'company' leer
            lassen). Struktur:
            [{"title":"","company":"","location":"","url":"","description":""}]
            Respond with ONLY the JSON array, nothing else.
            """;

    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    public LlmWebSearchSource(LlmService llmService, ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<RawJob> fetch(JobQuery query, Map<String, Object> config) {
        String user = "Suchbegriffe: " + query.keywordString()
                + (query.location() != null ? "\nRegion: " + query.location() : "")
                + "\nAnzahl: " + Math.min(query.size(), 15);
        try {
            var result = llmService.chat(
                    (java.util.UUID) null,
                    new ChatRequest(List.of(ChatMessage.system(SYSTEM), ChatMessage.user(user)), 0.3, 2000));
            JsonNode array = objectMapper.readTree(JsonExtractor.extract(result.content()));
            if (!array.isArray()) {
                return List.of();
            }
            List<RawJob> jobs = new ArrayList<>();
            for (JsonNode item : array) {
                String title = item.path("title").asText("").strip();
                if (title.isEmpty()) {
                    continue;
                }
                jobs.add(new RawJob(
                        CODE,
                        emptyToNull(item.path("url").asText("")),
                        title,
                        emptyToNull(item.path("company").asText("")),
                        emptyToNull(item.path("location").asText("")),
                        null,
                        emptyToNull(item.path("description").asText("")),
                        emptyToNull(item.path("url").asText("")),
                        null,
                        null,
                        item.toString()));
            }
            return jobs;
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
            log.info("LLM-Websuche skipped: {}", e.getMessage());
            return List.of();
        }
    }

    private String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
