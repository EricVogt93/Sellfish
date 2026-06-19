package de.sellfish.ai;

import de.sellfish.ai.LlmConfigDtos.ConfigRequest;
import de.sellfish.ai.model.ChatRequest;
import de.sellfish.ai.model.ResolvedModel;
import de.sellfish.ai.secret.SecretResolver;
import de.sellfish.common.crypto.CryptoService;
import de.sellfish.common.error.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LlmConfigService {

    private final LlmProviderConfigRepository repository;
    private final CryptoService cryptoService;
    private final SecretResolver secretResolver;
    private final LlmService llmService;

    public LlmConfigService(
            LlmProviderConfigRepository repository,
            CryptoService cryptoService,
            SecretResolver secretResolver,
            LlmService llmService) {
        this.repository = repository;
        this.cryptoService = cryptoService;
        this.secretResolver = secretResolver;
        this.llmService = llmService;
    }

    @Transactional(readOnly = true)
    public List<LlmProviderConfig> list(UUID userId) {
        return repository.findByUserId(userId);
    }

    @Transactional
    public LlmProviderConfig create(UUID userId, ConfigRequest req) {
        LlmProviderConfig config = new LlmProviderConfig(userId, req.provider(), req.model(), req.purpose());
        apply(config, req);
        return repository.save(config);
    }

    @Transactional
    public LlmProviderConfig update(UUID userId, UUID id, ConfigRequest req) {
        LlmProviderConfig config = owned(userId, id);
        config.setProvider(req.provider());
        config.setModel(req.model());
        config.setPurpose(req.purpose());
        apply(config, req);
        return repository.save(config);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        repository.delete(owned(userId, id));
    }

    public LlmConfigDtos.TestResult test(UUID userId, UUID id) {
        LlmProviderConfig config = owned(userId, id);
        ResolvedModel model = new ResolvedModel(
                config.getProvider(), config.getModel(), config.getBaseUrl(), secretResolver.resolveApiKey(config));
        try {
            if (config.getPurpose() == Purpose.EMBEDDING) {
                float[] vector = llmService.embed(model, "ping");
                return new LlmConfigDtos.TestResult(true, "Embedding-Dimension: " + vector.length);
            }
            var result = llmService.chat(model, ChatRequest.of("Antworte mit genau einem Wort.", "Sag 'pong'."));
            return new LlmConfigDtos.TestResult(
                    true, "Antwort: " + result.content().strip());
        } catch (RuntimeException e) {
            return new LlmConfigDtos.TestResult(false, e.getMessage());
        }
    }

    private void apply(LlmProviderConfig config, ConfigRequest req) {
        config.setBaseUrl(req.baseUrl());
        config.setKeyRef(req.keyRef());
        if (req.apiKey() != null && !req.apiKey().isBlank()) {
            config.setKeyEnc(cryptoService.encrypt(req.apiKey()));
        }
        if (req.params() != null) {
            config.setParams(req.params());
        }
        config.setDefault(Boolean.TRUE.equals(req.isDefault()));
        config.setEnabled(req.enabled() == null || req.enabled());
    }

    private LlmProviderConfig owned(UUID userId, UUID id) {
        LlmProviderConfig config =
                repository.findById(id).orElseThrow(() -> ApiException.notFound("Konfiguration nicht gefunden"));
        if (config.getUserId() == null || !config.getUserId().equals(userId)) {
            throw ApiException.notFound("Konfiguration nicht gefunden");
        }
        return config;
    }
}
