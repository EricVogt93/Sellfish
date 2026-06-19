package de.sellfish.ai.adapter.out;

import com.fasterxml.jackson.databind.JsonNode;
import de.sellfish.ai.LlmException;
import de.sellfish.ai.model.ResolvedModel;
import java.util.function.Consumer;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Shared HTTP execution for all LLM clients. Eliminates the duplicated
 * try/catch -> build client -> post -> null-check -> wrap exception pattern
 * that was copy-pasted across OllamaClient, OpenAiCompatibleClient,
 * AnthropicClient and GoogleGeminiClient.
 */
abstract class AbstractLlmHttpClient {

    protected final RestClient.Builder builder;

    protected AbstractLlmHttpClient(RestClient.Builder builder) {
        this.builder = builder;
    }

    /** Build a RestClient for the given model's base URL (or default). */
    protected RestClient client(ResolvedModel model) {
        return builder.baseUrl(baseUrl(model)).build();
    }

    /** Default base URL for this provider when model.baseUrl() is null/blank. */
    protected abstract String defaultBaseUrl();

    /** Resolve the base URL for a specific model instance. */
    protected String baseUrl(ResolvedModel model) {
        String base = (model.baseUrl() == null || model.baseUrl().isBlank()) ? defaultBaseUrl() : model.baseUrl();
        return base.replaceAll("/+$", "");
    }

    /** Execute a POST and return the JSON response, wrapping errors as LlmException. */
    protected JsonNode postJson(
            ResolvedModel model, String path, Object body, Consumer<RestClient.RequestBodySpec> headerCustomizer) {
        try {
            RestClient client = client(model);
            RestClient.RequestBodySpec spec = client.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
            if (headerCustomizer != null) {
                headerCustomizer.accept(spec);
            }
            JsonNode result = spec.retrieve().body(JsonNode.class);
            if (result == null) {
                throw new LlmException("Empty response from " + path);
            }
            return result;
        } catch (RestClientException e) {
            throw new LlmException("Request failed: " + e.getMessage(), e);
        }
    }

    /** Simple POST without extra header customization. */
    protected JsonNode postJson(ResolvedModel model, String path, Object body) {
        return postJson(model, path, body, null);
    }
}
