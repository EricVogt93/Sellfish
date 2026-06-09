package de.bewerbungsatze.ai;

import de.bewerbungsatze.ai.client.ChatProvider;
import de.bewerbungsatze.ai.client.EmbeddingProvider;
import de.bewerbungsatze.ai.model.ChatRequest;
import de.bewerbungsatze.ai.model.ChatResult;
import de.bewerbungsatze.ai.model.ResolvedModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Zentrale, provider-agnostische Fassade für Chat- und Embedding-Aufrufe.
 */
@Service
public class LlmService {

    private final ProviderResolver providerResolver;
    private final List<ChatProvider> chatProviders;
    private final List<EmbeddingProvider> embeddingProviders;

    public LlmService(ProviderResolver providerResolver,
                      List<ChatProvider> chatProviders,
                      List<EmbeddingProvider> embeddingProviders) {
        this.providerResolver = providerResolver;
        this.chatProviders = chatProviders;
        this.embeddingProviders = embeddingProviders;
    }

    public ChatResult chat(UUID userId, ChatRequest request) {
        ResolvedModel model = providerResolver.resolve(userId, Purpose.CHAT);
        return chat(model, request);
    }

    public ChatResult chat(ResolvedModel model, ChatRequest request) {
        return chatProvider(model.provider()).chat(model, request);
    }

    public float[] embed(UUID userId, String text) {
        ResolvedModel model = providerResolver.resolve(userId, Purpose.EMBEDDING);
        return embed(model, text);
    }

    public float[] embed(ResolvedModel model, String text) {
        return embeddingProvider(model.provider()).embed(model, text);
    }

    private ChatProvider chatProvider(Provider provider) {
        return chatProviders.stream()
                .filter(p -> p.supports(provider))
                .findFirst()
                .orElseThrow(() -> new LlmException("Kein Chat-Client für Provider " + provider));
    }

    private EmbeddingProvider embeddingProvider(Provider provider) {
        return embeddingProviders.stream()
                .filter(p -> p.supports(provider))
                .findFirst()
                .orElseThrow(() -> new LlmException(
                        "Provider " + provider + " unterstützt keine Embeddings"));
    }
}
