package de.sellfish.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.sellfish.ai.model.ResolvedModel;
import de.sellfish.ai.secret.SecretResolver;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProviderResolverTest {

    private final LlmProviderConfigRepository repository = mock(LlmProviderConfigRepository.class);
    private final SecretResolver secretResolver = mock(SecretResolver.class);
    private final ProviderResolver resolver = new ProviderResolver(repository, secretResolver);

    private LlmProviderConfig config(UUID userId, Provider provider, boolean isDefault) {
        LlmProviderConfig c = new LlmProviderConfig(userId, provider, "model-x", Purpose.CHAT);
        c.setDefault(isDefault);
        return c;
    }

    @Test
    void prefersUserConfigOverGlobal() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserIdAndPurposeAndEnabledTrue(userId, Purpose.CHAT))
                .thenReturn(List.of(config(userId, Provider.OLLAMA, false)));
        when(secretResolver.resolveApiKey(any())).thenReturn(null);

        ResolvedModel model = resolver.resolve(userId, Purpose.CHAT);

        assertThat(model.provider()).isEqualTo(Provider.OLLAMA);
    }

    @Test
    void prefersDefaultAmongUserConfigs() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserIdAndPurposeAndEnabledTrue(userId, Purpose.CHAT))
                .thenReturn(List.of(config(userId, Provider.OLLAMA, false), config(userId, Provider.OPENAI, true)));
        when(secretResolver.resolveApiKey(any())).thenReturn("k");

        assertThat(resolver.resolve(userId, Purpose.CHAT).provider()).isEqualTo(Provider.OPENAI);
    }

    @Test
    void fallsBackToGlobalWhenNoUserConfig() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserIdAndPurposeAndEnabledTrue(userId, Purpose.CHAT))
                .thenReturn(List.of());
        when(repository.findGlobalByPurpose(Purpose.CHAT)).thenReturn(List.of(config(null, Provider.ANTHROPIC, true)));
        when(secretResolver.resolveApiKey(any())).thenReturn("k");

        assertThat(resolver.resolve(userId, Purpose.CHAT).provider()).isEqualTo(Provider.ANTHROPIC);
    }

    @Test
    void throwsWhenNothingConfigured() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserIdAndPurposeAndEnabledTrue(userId, Purpose.CHAT))
                .thenReturn(List.of());
        when(repository.findGlobalByPurpose(Purpose.CHAT)).thenReturn(List.of());

        assertThatThrownBy(() -> resolver.resolve(userId, Purpose.CHAT)).isInstanceOf(LlmException.class);
    }

    @Test
    void resolvesApiKeyViaSecretResolver() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserIdAndPurposeAndEnabledTrue(userId, Purpose.CHAT))
                .thenReturn(List.of(config(userId, Provider.OPENAI, true)));
        when(secretResolver.resolveApiKey(any())).thenReturn("resolved-key");

        assertThat(resolver.resolve(userId, Purpose.CHAT).apiKey()).isEqualTo("resolved-key");
    }
}
