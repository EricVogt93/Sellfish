package de.sellfish.ai;

import de.sellfish.ai.model.ChatRequest;
import de.sellfish.ai.model.ChatResult;
import de.sellfish.ai.model.ResolvedModel;
import de.sellfish.ai.port.ChatProvider;
import de.sellfish.ai.port.EmbeddingProvider;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Zentrale, provider-agnostische Fassade für Chat- und Embedding-Aufrufe.
 */
@Service
public class LlmService {

    private final ProviderResolver providerResolver;
    private final List<ChatProvider> chatProviders;
    private final List<EmbeddingProvider> embeddingProviders;

    public LlmService(
            ProviderResolver providerResolver,
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
                .orElseThrow(() -> new LlmException("No chat client for provider " + provider));
    }

    private EmbeddingProvider embeddingProvider(Provider provider) {
        return embeddingProviders.stream()
                .filter(p -> p.supports(provider))
                .findFirst()
                .orElseThrow(() -> new LlmException("Provider " + provider + " does not support embeddings"));
    }
}
