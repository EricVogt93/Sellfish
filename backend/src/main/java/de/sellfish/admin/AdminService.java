package de.sellfish.admin;

import de.sellfish.ai.LlmProviderConfig;
import de.sellfish.ai.LlmProviderConfigRepository;
import de.sellfish.ai.Provider;
import de.sellfish.ai.Purpose;
import de.sellfish.common.crypto.CryptoService;
import de.sellfish.common.error.ApiException;
import de.sellfish.jobs.JobSourceConfig;
import de.sellfish.jobs.JobSourceConfigRepository;
import de.sellfish.users.Role;
import de.sellfish.users.User;
import de.sellfish.users.UserRepository;
import de.sellfish.users.UserStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administrative functions: user management, job sources and global LLM configuration.
 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final JobSourceConfigRepository jobSourceRepository;
    private final LlmProviderConfigRepository llmConfigRepository;
    private final CryptoService cryptoService;

    public AdminService(
            UserRepository userRepository,
            JobSourceConfigRepository jobSourceRepository,
            LlmProviderConfigRepository llmConfigRepository,
            CryptoService cryptoService) {
        this.userRepository = userRepository;
        this.jobSourceRepository = jobSourceRepository;
        this.llmConfigRepository = llmConfigRepository;
        this.cryptoService = cryptoService;
    }

    @Transactional(readOnly = true)
    public List<User> listUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User setRole(UUID userId, Role role) {
        User user = user(userId);
        user.setRole(role);
        return userRepository.save(user);
    }

    @Transactional
    public User setStatus(UUID userId, UserStatus status) {
        User user = user(userId);
        user.setStatus(status);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<JobSourceConfig> listJobSources() {
        return jobSourceRepository.findAll();
    }

    @Transactional
    public JobSourceConfig updateJobSource(String code, Boolean enabled, String config) {
        JobSourceConfig source = jobSourceRepository
                .findByCode(code)
                .orElseThrow(() -> ApiException.notFound("Job source not found: " + code));
        if (enabled != null) {
            source.setEnabled(enabled);
        }
        if (config != null) {
            source.setConfig(config);
        }
        return jobSourceRepository.save(source);
    }

    @Transactional(readOnly = true)
    public List<LlmProviderConfig> listGlobalLlmConfigs() {
        return llmConfigRepository.findAllGlobal();
    }

    @Transactional
    public LlmProviderConfig createGlobalLlmConfig(
            Provider provider,
            String model,
            Purpose purpose,
            String baseUrl,
            String keyRef,
            String apiKey,
            boolean isDefault) {
        List<LlmProviderConfig> globals = llmConfigRepository.findAllGlobal();

        // API-Key pro Provider (baseUrl) wiederverwenden: bleibt das Feld leer, ziehen wir den
        // reuse the stored key of the same baseUrl. This lets you switch the model via the dropdown
        // umschalten, without den Schluessel jedes Mal neu einzugeben.
        String keyEnc = null;
        if (apiKey != null && !apiKey.isBlank()) {
            keyEnc = cryptoService.encrypt(apiKey);
        } else if (baseUrl != null) {
            keyEnc = globals.stream()
                    .filter(c -> baseUrl.equals(c.getBaseUrl())
                            && c.getKeyEnc() != null
                            && !c.getKeyEnc().isBlank())
                    .map(LlmProviderConfig::getKeyEnc)
                    .findFirst()
                    .orElse(null);
        }

        // Upsert: vorhandene globale Config mit gleicher purpose+baseUrl+model wiederverwenden,
        // statt Duplikate anzulegen.
        LlmProviderConfig config = globals.stream()
                .filter(c -> c.getPurpose() == purpose
                        && java.util.Objects.equals(c.getBaseUrl(), baseUrl)
                        && java.util.Objects.equals(c.getModel(), model))
                .findFirst()
                .orElseGet(() -> new LlmProviderConfig(null, provider, model, purpose));
        config.setProvider(provider);
        config.setModel(model);
        config.setBaseUrl(baseUrl);
        if (keyRef != null && !keyRef.isBlank()) {
            config.setKeyRef(keyRef);
        }
        if (keyEnc != null) {
            config.setKeyEnc(keyEnc);
        }

        // Genau ein Default pro Purpose: andere Defaults derselben Purpose zuruecksetzen.
        if (isDefault) {
            for (LlmProviderConfig other : globals) {
                if (other.getPurpose() == purpose && other.isDefault() && other != config) {
                    other.setDefault(false);
                    llmConfigRepository.save(other);
                }
            }
        }
        config.setDefault(isDefault);
        return llmConfigRepository.save(config);
    }

    @Transactional
    public void deleteGlobalLlmConfig(UUID id) {
        LlmProviderConfig config =
                llmConfigRepository.findById(id).orElseThrow(() -> ApiException.notFound("Configuration not found"));
        if (config.getUserId() != null) {
            throw ApiException.badRequest("No global configuration");
        }
        llmConfigRepository.delete(config);
    }

    private User user(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
    }
}
