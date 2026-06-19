package de.sellfish.account;

import de.sellfish.ai.LlmProviderConfig;
import de.sellfish.ai.LlmProviderConfigRepository;
import de.sellfish.common.error.ApiException;
import de.sellfish.cv.CvStructuredRepository;
import de.sellfish.cv.ProjectRepository;
import de.sellfish.docs.Document;
import de.sellfish.docs.DocumentRepository;
import de.sellfish.feedback.FeedbackEventRepository;
import de.sellfish.generate.GeneratedDocument;
import de.sellfish.generate.GeneratedDocumentRepository;
import de.sellfish.matching.JobMatch;
import de.sellfish.matching.JobMatchRepository;
import de.sellfish.profile.PreferencesRepository;
import de.sellfish.profile.ProfileRepository;
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
 * DSGVO-Funktionen: vollständiger Datenexport und Account-Löschung eines Nutzers.
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PreferencesRepository preferencesRepository;
    private final CvStructuredRepository cvRepository;
    private final ProjectRepository projectRepository;
    private final DocumentRepository documentRepository;
    private final JobMatchRepository matchRepository;
    private final GeneratedDocumentRepository generatedRepository;
    private final FeedbackEventRepository feedbackRepository;
    private final LlmProviderConfigRepository llmConfigRepository;
    private final StorageService storage;

    public AccountService(
            UserRepository userRepository,
            ProfileRepository profileRepository,
            PreferencesRepository preferencesRepository,
            CvStructuredRepository cvRepository,
            ProjectRepository projectRepository,
            DocumentRepository documentRepository,
            JobMatchRepository matchRepository,
            GeneratedDocumentRepository generatedRepository,
            FeedbackEventRepository feedbackRepository,
            LlmProviderConfigRepository llmConfigRepository,
            StorageService storage) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.preferencesRepository = preferencesRepository;
        this.cvRepository = cvRepository;
        this.projectRepository = projectRepository;
        this.documentRepository = documentRepository;
        this.matchRepository = matchRepository;
        this.generatedRepository = generatedRepository;
        this.feedbackRepository = feedbackRepository;
        this.llmConfigRepository = llmConfigRepository;
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

        data.put("profile", profileRepository.findByUserId(userId).orElse(null));
        data.put("preferences", preferencesRepository.findByUserId(userId).orElse(null));
        data.put("cv", cvRepository.findByUserId(userId).orElse(null));
        data.put("projects", projectRepository.findByUserIdOrderByCreatedAtDesc(userId));
        data.put(
                "documents",
                documentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                        .map(this::documentView)
                        .toList());
        data.put(
                "matches",
                matchRepository.findByUserId(userId).stream()
                        .map(this::matchView)
                        .toList());
        data.put("generatedDocuments", generatedRepository.findByUserIdOrderByCreatedAtDesc(userId));
        data.put("feedback", feedbackRepository.findByUserIdOrderByTsDesc(userId));
        data.put(
                "llmProviders",
                llmConfigRepository.findByUserId(userId).stream()
                        .map(this::providerView)
                        .toList());
        return data;
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw ApiException.notFound("User not found");
        }
        // Dateien zuerst löschen (kein DB-Cascade in den Objekt-Storage).
        for (Document doc : documentRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            safeDelete(doc.getStorageKey());
        }
        for (GeneratedDocument doc : generatedRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            safeDelete(doc.getStorageKey());
        }
        // Alle übrigen Daten per FK ON DELETE CASCADE.
        userRepository.deleteById(userId);
    }

    private void safeDelete(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            storage.delete(key);
        } catch (RuntimeException e) {
            log.warn("storage object {} could not be deleted: {}", key, e.getMessage());
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
        // Bewusst without Schlüsselmaterial.
        return v;
    }
}
