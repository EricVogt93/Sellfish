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
 * Client für OpenAI und alle OpenAI-kompatiblen Endpunkte (NVIDIA NIM, Kimi, OpenRouter, …).
 */
@Component
public class OpenAiCompatibleClient implements ChatProvider, EmbeddingProvider {

    private final RestClient.Builder builder;

    public OpenAiCompatibleClient(RestClient.Builder builder) {
        this.builder = builder;
    }

    @Override
    public boolean supports(Provider provider) {
        return provider == Provider.OPENAI
                || provider == Provider.NIM
                || provider == Provider.OPENAI_COMPATIBLE;
    }

    private String baseUrl(ResolvedModel model) {
        if (model.baseUrl() != null && !model.baseUrl().isBlank()) {
            return model.baseUrl().replaceAll("/+$", "");
        }
        return switch (model.provider()) {
            case OPENAI -> "https://api.openai.com/v1";
            case NIM -> "https://integrate.api.nvidia.com/v1";
            default -> throw new LlmException(
                    "base_url ist für Provider " + model.provider() + " erforderlich");
        };
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
        if (request.temperature() != null) {
            body.put("temperature", request.temperature());
        }
        if (request.maxTokens() != null) {
            body.put("max_tokens", request.maxTokens());
        }

        JsonNode response = post(model, "/chat/completions", body);
        JsonNode choice = response.path("choices").path(0).path("message").path("content");
        if (choice.isMissingNode()) {
            throw new LlmException("Unerwartete Antwort vom OpenAI-kompatiblen Endpunkt");
        }
        JsonNode usage = response.path("usage");
        return new ChatResult(
                choice.asText(),
                response.path("model").asText(model.model()),
                usage.path("prompt_tokens").isNumber() ? usage.get("prompt_tokens").asInt() : null,
                usage.path("completion_tokens").isNumber() ? usage.get("completion_tokens").asInt() : null);
    }

    @Override
    public float[] embed(ResolvedModel model, String text) {
        Map<String, Object> body = Map.of("model", model.model(), "input", text);
        JsonNode response = post(model, "/embeddings", body);
        JsonNode vector = response.path("data").path(0).path("embedding");
        if (!vector.isArray()) {
            throw new LlmException("Embedding-Antwort enthält keinen Vektor");
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
                    .headers(h -> {
                        if (model.apiKey() != null && !model.apiKey().isBlank()) {
                            h.setBearerAuth(model.apiKey());
                        }
                    })
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (result == null) {
                throw new LlmException("Leere Antwort vom Endpunkt " + path);
            }
            return result;
        } catch (RestClientException e) {
            throw new LlmException("LLM-Aufruf fehlgeschlagen (" + model.provider() + "): " + e.getMessage(), e);
        }
    }
}
