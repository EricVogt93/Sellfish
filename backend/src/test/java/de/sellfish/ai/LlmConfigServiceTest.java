package de.sellfish.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.sellfish.ai.LlmConfigDtos.ConfigRequest;
import de.sellfish.ai.LlmConfigDtos.TestResult;
import de.sellfish.ai.model.ChatRequest;
import de.sellfish.ai.model.ChatResult;
import de.sellfish.ai.model.ResolvedModel;
import de.sellfish.ai.secret.SecretResolver;
import de.sellfish.common.crypto.CryptoService;
import de.sellfish.common.error.ApiException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LlmConfigServiceTest {

    @Mock
    LlmProviderConfigRepository repository;

    @Mock
    CryptoService cryptoService;

    @Mock
    SecretResolver secretResolver;

    @Mock
    LlmService llmService;

    @InjectMocks
    LlmConfigService service;

    private ConfigRequest req() {
        return new ConfigRequest(
                Provider.OPENAI, "gpt-4o", "https://api.openai.com", null, "sk-key", Purpose.CHAT, true, true, null);
    }

    @Test
    void listDelegatesToRepository() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserId(userId)).thenReturn(List.of());
        assertThat(service.list(userId)).isEmpty();
    }

    @Test
    void createEncryptsApiKeyAndSaves() {
        UUID userId = UUID.randomUUID();
        when(cryptoService.encrypt("sk-key")).thenReturn("enc");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        LlmProviderConfig saved = service.create(userId, req());
        verify(cryptoService).encrypt("sk-key");
        assertThat(saved.getModel()).isEqualTo("gpt-4o");
        assertThat(saved.isDefault()).isTrue();
    }

    @Test
    void testChatReturnsSuccessResult() {
        UUID userId = UUID.randomUUID();
        LlmProviderConfig config = mock(LlmProviderConfig.class);
        when(config.getUserId()).thenReturn(userId);
        when(config.getPurpose()).thenReturn(Purpose.CHAT);
        when(config.getProvider()).thenReturn(Provider.OPENAI);
        when(config.getModel()).thenReturn("gpt-4o");
        when(secretResolver.resolveApiKey(config)).thenReturn("sk");
        when(repository.findById(any())).thenReturn(Optional.of(config));
        when(llmService.chat(any(ResolvedModel.class), any(ChatRequest.class)))
                .thenReturn(new ChatResult("pong", "gpt-4o", null, null));

        TestResult result = service.test(userId, UUID.randomUUID());
        assertThat(result.ok()).isTrue();
        assertThat(result.message()).contains("pong");
    }

    @Test
    void testEmbedReturnsDimension() {
        UUID userId = UUID.randomUUID();
        LlmProviderConfig config = mock(LlmProviderConfig.class);
        when(config.getUserId()).thenReturn(userId);
        when(config.getPurpose()).thenReturn(Purpose.EMBEDDING);
        when(secretResolver.resolveApiKey(config)).thenReturn("sk");
        when(repository.findById(any())).thenReturn(Optional.of(config));
        when(llmService.embed(any(ResolvedModel.class), anyString())).thenReturn(new float[768]);

        TestResult result = service.test(userId, UUID.randomUUID());
        assertThat(result.ok()).isTrue();
        assertThat(result.message()).contains("768");
    }

    @Test
    void ownedThrowsForOtherUsersConfig() {
        UUID userId = UUID.randomUUID();
        LlmProviderConfig others = mock(LlmProviderConfig.class);
        when(others.getUserId()).thenReturn(UUID.randomUUID());
        when(repository.findById(any())).thenReturn(Optional.of(others));
        assertThatThrownBy(() -> service.test(userId, UUID.randomUUID())).isInstanceOf(ApiException.class);
    }
}
