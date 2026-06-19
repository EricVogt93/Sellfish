package de.sellfish.ai.adapter.out;

import com.fasterxml.jackson.databind.JsonNode;
import de.sellfish.ai.LlmException;
import de.sellfish.ai.Provider;
import de.sellfish.ai.model.ChatMessage;
import de.sellfish.ai.model.ChatRequest;
import de.sellfish.ai.model.ChatResult;
import de.sellfish.ai.model.ResolvedModel;
import de.sellfish.ai.port.ChatProvider;
import de.sellfish.ai.port.EmbeddingProvider;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Client für Google Gemini (Generative Language API).
 */
@Component
public class GoogleGeminiClient implements ChatProvider, EmbeddingProvider {

    private final RestClient.Builder builder;

    public GoogleGeminiClient(RestClient.Builder builder) {
        this.builder = builder;
    }

    @Override
    public boolean supports(Provider provider) {
        return provider == Provider.GOOGLE;
    }

    private String baseUrl(ResolvedModel model) {
        String base = (model.baseUrl() == null || model.baseUrl().isBlank())
                ? "https://generativelanguage.googleapis.com/v1beta"
                : model.baseUrl();
        return base.replaceAll("/+$", "");
    }

    @Override
    public ChatResult chat(ResolvedModel model, ChatRequest request) {
        String system = request.messages().stream()
                .filter(m -> "system".equals(m.role()))
                .map(ChatMessage::content)
                .collect(Collectors.joining("\n\n"));

        List<Map<String, Object>> contents = new ArrayList<>();
        for (ChatMessage m : request.messages()) {
            if ("system".equals(m.role())) {
                continue;
            }
            String role = "assistant".equals(m.role()) ? "model" : "user";
            contents.add(Map.of("role", role, "parts", List.of(Map.of("text", m.content()))));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", contents);
        if (!system.isBlank()) {
            body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", system))));
        }
        Map<String, Object> genConfig = new LinkedHashMap<>();
        if (request.temperature() != null) {
            genConfig.put("temperature", request.temperature());
        }
        if (request.maxTokens() != null) {
            genConfig.put("maxOutputTokens", request.maxTokens());
        }
        if (!genConfig.isEmpty()) {
            body.put("generationConfig", genConfig);
        }

        JsonNode response = post(model, "/models/" + model.model() + ":generateContent", body);
        JsonNode text = response.path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text");
        if (text.isMissingNode()) {
            throw new LlmException("Unerwartete Gemini-Antwort");
        }
        JsonNode usage = response.path("usageMetadata");
        return new ChatResult(
                text.asText(),
                model.model(),
                usage.path("promptTokenCount").isNumber()
                        ? usage.get("promptTokenCount").asInt()
                        : null,
                usage.path("candidatesTokenCount").isNumber()
                        ? usage.get("candidatesTokenCount").asInt()
                        : null);
    }

    @Override
    public float[] embed(ResolvedModel model, String text) {
        Map<String, Object> body = Map.of("content", Map.of("parts", List.of(Map.of("text", text))));
        JsonNode response = post(model, "/models/" + model.model() + ":embedContent", body);
        JsonNode vector = response.path("embedding").path("values");
        if (!vector.isArray()) {
            throw new LlmException("Gemini embedding response contains no vector");
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
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("key", model.apiKey())
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (result == null) {
                throw new LlmException("Leere Gemini-Antwort von " + path);
            }
            return result;
        } catch (RestClientException e) {
            throw new LlmException("Gemini-Aufruf failed: " + e.getMessage(), e);
        }
    }
}
