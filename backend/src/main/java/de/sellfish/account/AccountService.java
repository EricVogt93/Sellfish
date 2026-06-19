package de.sellfish.account;

import de.sellfish.ai.LlmProviderConfig;
import de.sellfish.common.error.ApiException;
import de.sellfish.docs.Document;
import de.sellfish.generate.GeneratedDocument;
import de.sellfish.matching.JobMatch;
import de.sellfish.storage.port.StorageService;
import de.sellfish.users.User;
import de.sellfish.users.UserRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GDPR functions: full data export and account deletion.
 * Uses UserDataReaders facade (3 deps) instead of 11 individual repositories.
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final UserRepository userRepository;
    private final UserDataReaders readers;
    private final StorageService storage;

    public AccountService(UserRepository userRepository, UserDataReaders readers, StorageService storage) {
        this.userRepository = userRepository;
        this.readers = readers;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> export(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));

        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> account = new LinkedHashMap<>();
        account.put("id", user.getId());
        account.put("email", user.getEmail());
        account.put("role", user.getRole().name());
        account.put("locale", user.getLocale());
        account.put("createdAt", user.getCreatedAt());
        data.put("account", account);

        data.put("profile", readers.profiles().findByUserId(userId).orElse(null));
        data.put("preferences", readers.preferences().findByUserId(userId).orElse(null));
        data.put("cv", readers.cv().findByUserId(userId).orElse(null));
        data.put("projects", readers.projects().findByUserIdOrderByCreatedAtDesc(userId));
        data.put(
                "documents",
                readers.documents().findByUserIdOrderByCreatedAtDesc(userId).stream()
                        .map(this::documentView)
                        .toList());
        data.put(
                "matches",
                readers.matches().findByUserId(userId).stream()
                        .map(this::matchView)
                        .toList());
        data.put("generatedDocuments", readers.generatedDocs().findByUserIdOrderByCreatedAtDesc(userId));
        data.put("feedback", readers.feedback().findByUserIdOrderByTsDesc(userId));
        data.put(
                "llmProviders",
                readers.llmConfigs().findByUserId(userId).stream()
                        .map(this::providerView)
                        .toList());
        return data;
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw ApiException.notFound("User not found");
        }
        // Delete files first (no DB cascade into object storage).
        for (Document doc : readers.documents().findByUserIdOrderByCreatedAtDesc(userId)) {
            safeDelete(doc.getStorageKey());
        }
        for (GeneratedDocument doc : readers.generatedDocs().findByUserIdOrderByCreatedAtDesc(userId)) {
            safeDelete(doc.getStorageKey());
        }
        // All other data via FK ON DELETE CASCADE.
        userRepository.deleteById(userId);
    }

    private void safeDelete(String key) {
        if (key == null || key.isBlank()) return;
        try {
            storage.delete(key);
        } catch (RuntimeException e) {
            log.warn("Storage object {} could not be deleted: {}", key, e.getMessage());
        }
    }

    private Map<String, Object> documentView(Document d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("type", d.getType());
        m.put("filename", d.getFilename());
        m.put("sizeBytes", d.getSizeBytes());
        m.put("primary", d.isPrimary());
        m.put("createdAt", d.getCreatedAt());
        return m;
    }

    private Map<String, Object> matchView(JobMatch m) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("jobId", m.getJobId());
        v.put("score", m.getScore());
        v.put("rank", m.getRank());
        v.put("status", m.getStatus());
        return v;
    }

    private Map<String, Object> providerView(LlmProviderConfig c) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("provider", c.getProvider());
        v.put("model", c.getModel());
        v.put("purpose", c.getPurpose());
        v.put("baseUrl", c.getBaseUrl());
        // Intentionally without key material.
        return v;
    }
}
