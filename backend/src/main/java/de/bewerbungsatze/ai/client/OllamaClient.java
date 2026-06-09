package de.bewerbungsatze.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import de.bewerbungsatze.ai.LlmException;
import de.bewerbungsatze.ai.Provider;
import de.bewerbungsatze.ai.model.ChatMessage;
import de.bewerbungsatze.ai.model.ChatRequest;
import de.bewerbungsatze.ai.model.ChatResult;
import de.bewerbungsatze.ai.model.ResolvedModel;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client für self-hosted Ollama-Instanzen.
 */
@Component
public class OllamaClient implements ChatProvider, EmbeddingProvider {

    private final RestClient.Builder builder;

    public OllamaClient(RestClient.Builder builder) {
        this.builder = builder;
    }

    @Override
    public boolean supports(Provider provider) {
        return provider == Provider.OLLAMA;
    }

    private String baseUrl(ResolvedModel model) {
        String base = (model.baseUrl() == null || model.baseUrl().isBlank())
                ? "http://localhost:11434" : model.baseUrl();
        return base.replaceAll("/+$", "");
    }

    @Override
    public ChatResult chat(ResolvedModel model, ChatRequest request) {
        List<Map<String, Object>> messages = new ArrayList<>();
        for (ChatMessage m : request.messages()) {
            messages.add(Map.of("role", m.role(), "content", m.content()));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model.model());
        body.put("messages", messages);
        body.put("stream", false);
        if (request.temperature() != null) {
            body.put("options", Map.of("temperature", request.temperature()));
        }

        JsonNode response = post(model, "/api/chat", body);
        JsonNode content = response.path("message").path("content");
        if (content.isMissingNode()) {
            throw new LlmException("Unerwartete Ollama-Antwort");
        }
        return new ChatResult(
                content.asText(),
                response.path("model").asText(model.model()),
                response.path("prompt_eval_count").isNumber() ? response.get("prompt_eval_count").asInt() : null,
                response.path("eval_count").isNumber() ? response.get("eval_count").asInt() : null);
    }

    @Override
    public float[] embed(ResolvedModel model, String text) {
        Map<String, Object> body = Map.of("model", model.model(), "prompt", text);
        JsonNode response = post(model, "/api/embeddings", body);
        JsonNode vector = response.path("embedding");
        if (!vector.isArray()) {
            throw new LlmException("Ollama-Embedding-Antwort enthält keinen Vektor");
        }
        float[] out = new float[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            out[i] = (float) vector.get(i).asDouble();
        }
        return out;
    }

    private JsonNode post(ResolvedModel model, String path, Object body) {
        try {
            RestClient client = builder.baseUrl(baseUrl(model)).build();
            JsonNode result = client.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (result == null) {
                throw new LlmException("Leere Ollama-Antwort von " + path);
            }
            return result;
        } catch (RestClientException e) {
            throw new LlmException("Ollama-Aufruf fehlgeschlagen: " + e.getMessage(), e);
        }
    }
}
