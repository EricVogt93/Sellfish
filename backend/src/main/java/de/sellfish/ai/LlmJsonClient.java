package de.sellfish.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.sellfish.ai.model.ChatRequest;
import de.sellfish.ai.model.ChatResult;
import de.sellfish.common.json.JsonExtractor;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Helper for the common LLM pattern: send a prompt expecting a JSON response,
 * extract and parse it. Eliminates duplicated try/catch → fence-strip → parse
 * → fallback boilerplate across SmartImportService, AutoSetupService,
 * SalaryEstimateService and JobValidationService.
 */
@Component
public class LlmJsonClient {

    private static final Logger log = LoggerFactory.getLogger(LlmJsonClient.class);

    private final LlmService llmService;
    private final ObjectMapper mapper;

    public LlmJsonClient(LlmService llmService, ObjectMapper mapper) {
        this.llmService = llmService;
        this.mapper = mapper;
    }

    /**
     * Ask the LLM a question and parse the response as JSON.
     * Returns the parsed JsonNode, or null if the call fails or the response
     * is not valid JSON.
     */
    public JsonNode askJson(UUID userId, String systemPrompt, String userPrompt) {
        try {
            ChatResult result = llmService.chat(userId, ChatRequest.of(systemPrompt, userPrompt));
            String json = JsonExtractor.extract(result.content());
            return mapper.readTree(json);
        } catch (Exception e) {
            log.debug("LlmJsonClient failed for {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Ask and extract a string array field from the JSON response.
     */
    public java.util.List<String> askStringArray(UUID userId, String systemPrompt, String userPrompt, String field) {
        JsonNode node = askJson(userId, systemPrompt, userPrompt);
        if (node == null) return java.util.List.of();
        java.util.List<String> result = new java.util.ArrayList<>();
        node.path(field).forEach(v -> result.add(v.asText().trim()));
        return result.stream().filter(s -> !s.isBlank()).toList();
    }
}
