package de.bewerbungsatze.admin;

import de.bewerbungsatze.ai.LlmProviderConfig;
import de.bewerbungsatze.ai.LlmProviderConfigRepository;
import de.bewerbungsatze.ai.Provider;
import de.bewerbungsatze.ai.Purpose;
import de.bewerbungsatze.common.crypto.CryptoService;
import de.bewerbungsatze.common.error.ApiException;
import de.bewerbungsatze.jobs.JobSourceConfig;
import de.bewerbungsatze.jobs.JobSourceConfigRepository;
import de.bewerbungsatze.users.Role;
import de.bewerbungsatze.users.User;
import de.bewerbungsatze.users.UserRepository;
import de.bewerbungsatze.users.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Administrative Funktionen: Nutzerverwaltung, Job-Quellen und globale LLM-Konfiguration.
 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final JobSourceConfigRepository jobSourceRepository;
    private final LlmProviderConfigRepository llmConfigRepository;
    private final CryptoService cryptoService;

    public AdminService(UserRepository userRepository,
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
        JobSourceConfig source = jobSourceRepository.findByCode(code)
                .orElseThrow(() -> ApiException.notFound("Job-Quelle nicht gefunden: " + code));
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
    public LlmProviderConfig createGlobalLlmConfig(Provider provider, String model, Purpose purpose,
                                                   String baseUrl, String keyRef, String apiKey,
                                                   boolean isDefault) {
        LlmProviderConfig config = new LlmProviderConfig(null, provider, model, purpose);
        config.setBaseUrl(baseUrl);
        config.setKeyRef(keyRef);
        if (apiKey != null && !apiKey.isBlank()) {
            config.setKeyEnc(cryptoService.encrypt(apiKey));
        }
        config.setDefault(isDefault);
        return llmConfigRepository.save(config);
    }

    @Transactional
    public void deleteGlobalLlmConfig(UUID id) {
        LlmProviderConfig config = llmConfigRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Konfiguration nicht gefunden"));
        if (config.getUserId() != null) {
            throw ApiException.badRequest("Keine globale Konfiguration");
        }
        llmConfigRepository.delete(config);
    }

    private User user(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Nutzer nicht gefunden"));
    }
}
