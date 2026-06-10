package de.bewerbungsatze.ai.adapter.out;
import de.bewerbungsatze.ai.port.ChatProvider;
import de.bewerbungsatze.ai.port.EmbeddingProvider;

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
import java.util.stream.Collectors;

/**
 * Client für Anthropic (Claude). Embeddings werden von Anthropic nicht angeboten.
 */
@Component
public class AnthropicClient implements ChatProvider {

    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient.Builder builder;

    public AnthropicClient(RestClient.Builder builder) {
        this.builder = builder;
    }

    @Override
    public boolean supports(Provider provider) {
        return provider == Provider.ANTHROPIC;
    }

    private String baseUrl(ResolvedModel model) {
        String base = (model.baseUrl() == null || model.baseUrl().isBlank())
                ? "https://api.anthropic.com" : model.baseUrl();
        return base.replaceAll("/+$", "");
    }

    @Override
    public ChatResult chat(ResolvedModel model, ChatRequest request) {
        // System-Nachrichten zusammenfassen; Anthropic erwartet sie als Top-Level-Feld.
        String system = request.messages().stream()
                .filter(m -> "system".equals(m.role()))
                .map(ChatMessage::content)
                .collect(Collectors.joining("\n\n"));

        List<Map<String, Object>> messages = new ArrayList<>();
        for (ChatMessage m : request.messages()) {
            if ("system".equals(m.role())) {
                continue;
            }
            messages.add(Map.of("role", m.role(), "content", m.content()));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model.model());
        body.put("max_tokens", request.maxTokens() != null ? request.maxTokens() : 2048);
        if (!system.isBlank()) {
            body.put("system", system);
        }
        if (request.temperature() != null) {
            body.put("temperature", request.temperature());
        }
        body.put("messages", messages);

        try {
            RestClient client = builder.baseUrl(baseUrl(model)).build();
            JsonNode response = client.post()
                    .uri("/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-api-key", model.apiKey())
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                throw new LlmException("Leere Anthropic-Antwort");
            }
            JsonNode text = response.path("content").path(0).path("text");
            if (text.isMissingNode()) {
                throw new LlmException("Unerwartete Anthropic-Antwort");
            }
            JsonNode usage = response.path("usage");
            return new ChatResult(
                    text.asText(),
                    response.path("model").asText(model.model()),
                    usage.path("input_tokens").isNumber() ? usage.get("input_tokens").asInt() : null,
                    usage.path("output_tokens").isNumber() ? usage.get("output_tokens").asInt() : null);
        } catch (RestClientException e) {
            throw new LlmException("Anthropic-Aufruf fehlgeschlagen: " + e.getMessage(), e);
        }
    }
}
