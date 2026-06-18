package de.sellfish.admin;

import de.sellfish.ai.LlmProviderConfig;
import de.sellfish.ai.LlmProviderConfigRepository;
import de.sellfish.ai.Provider;
import de.sellfish.ai.Purpose;
import de.sellfish.common.config.CryptoProperties;
import de.sellfish.common.crypto.CryptoService;
import de.sellfish.common.error.ApiException;
import de.sellfish.jobs.JobSourceConfig;
import de.sellfish.jobs.JobSourceConfigRepository;
import de.sellfish.users.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final JobSourceConfigRepository jobSourceRepository = mock(JobSourceConfigRepository.class);
    private final LlmProviderConfigRepository llmConfigRepository = mock(LlmProviderConfigRepository.class);
    private final CryptoService cryptoService = new CryptoService(
            new CryptoProperties(Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes())));

    private final AdminService service = new AdminService(
            userRepository, jobSourceRepository, llmConfigRepository, cryptoService);

    @Test
    void updateJobSourceTogglesEnabled() {
        JobSourceConfig cfg = mock(JobSourceConfig.class);
        when(jobSourceRepository.findByCode("BA")).thenReturn(Optional.of(cfg));
        when(jobSourceRepository.save(cfg)).thenReturn(cfg);

        service.updateJobSource("BA", true, "{\"x\":1}");

        verify(cfg).setEnabled(true);
        verify(cfg).setConfig("{\"x\":1}");
    }

    @Test
    void updateUnknownJobSourceThrows() {
        when(jobSourceRepository.findByCode("NOPE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateJobSource("NOPE", true, null))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void createGlobalConfigEncryptsApiKey() {
        when(llmConfigRepository.save(any(LlmProviderConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        LlmProviderConfig saved = service.createGlobalLlmConfig(
                Provider.OPENAI, "gpt-4o", Purpose.CHAT, null, null, "sk-secret", true);

        assertThat(saved.getUserId()).isNull();
        assertThat(saved.getKeyEnc()).isNotNull();
        assertThat(cryptoService.decrypt(saved.getKeyEnc())).isEqualTo("sk-secret");
        assertThat(saved.isDefault()).isTrue();
    }

    @Test
    void deleteRejectsNonGlobalConfig() {
        UUID id = UUID.randomUUID();
        LlmProviderConfig userConfig = new LlmProviderConfig(UUID.randomUUID(), Provider.OLLAMA, "m", Purpose.CHAT);
        when(llmConfigRepository.findById(id)).thenReturn(Optional.of(userConfig));

        assertThatThrownBy(() -> service.deleteGlobalLlmConfig(id)).isInstanceOf(ApiException.class);
        verify(llmConfigRepository, never()).delete(any());
    }
}
