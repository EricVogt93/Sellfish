package de.sellfish.ai;

import de.sellfish.ai.port.ChatProvider;
import de.sellfish.ai.port.EmbeddingProvider;
import de.sellfish.ai.model.ChatRequest;
import de.sellfish.ai.model.ChatResult;
import de.sellfish.ai.model.ResolvedModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmServiceTest {

    private final ProviderResolver resolver = mock(ProviderResolver.class);

    private ChatProvider chatProviderFor(Provider provider, ChatResult result) {
        ChatProvider p = mock(ChatProvider.class);
        when(p.supports(provider)).thenReturn(true);
        when(p.chat(any(), any())).thenReturn(result);
        return p;
    }

    @Test
    void dispatchesChatToSupportingProvider() {
        ResolvedModel model = new ResolvedModel(Provider.OPENAI, "gpt-4o", null, "k");
        when(resolver.resolve(any(), any())).thenReturn(model);

        ChatProvider openai = chatProviderFor(Provider.OPENAI, new ChatResult("ok", "gpt-4o", 1, 1));
        LlmService service = new LlmService(resolver, List.of(openai), List.of());

        ChatResult result = service.chat(UUID.randomUUID(), ChatRequest.of("s", "u"));
        assertThat(result.content()).isEqualTo("ok");
    }

    @Test
    void throwsWhenNoChatProviderSupportsProvider() {
        ResolvedModel model = new ResolvedModel(Provider.GOOGLE, "gemini", null, "k");
        when(resolver.resolve(any(), any())).thenReturn(model);

        ChatProvider openai = mock(ChatProvider.class);
        when(openai.supports(any())).thenReturn(false);
        LlmService service = new LlmService(resolver, List.of(openai), List.of());

        assertThatThrownBy(() -> service.chat(UUID.randomUUID(), ChatRequest.of("s", "u")))
                .isInstanceOf(LlmException.class);
    }

    @Test
    void dispatchesEmbedToSupportingProvider() {
        ResolvedModel model = new ResolvedModel(Provider.OLLAMA, "nomic", null, null);
        when(resolver.resolve(any(), any())).thenReturn(model);

        EmbeddingProvider emb = mock(EmbeddingProvider.class);
        when(emb.supports(Provider.OLLAMA)).thenReturn(true);
        when(emb.embed(any(), any())).thenReturn(new float[]{1f, 2f});
        LlmService service = new LlmService(resolver, List.of(), List.of(emb));

        assertThat(service.embed(UUID.randomUUID(), "text")).containsExactly(1f, 2f);
    }
}
