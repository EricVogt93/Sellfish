package de.sellfish.generate;

import de.sellfish.ai.LlmService;
import de.sellfish.ai.model.ChatMessage;
import de.sellfish.ai.model.ChatRequest;
import de.sellfish.ai.model.ChatResult;
import de.sellfish.common.error.ApiException;
import de.sellfish.jobs.Job;
import de.sellfish.jobs.JobRepository;
import de.sellfish.matching.JobMatch;
import de.sellfish.matching.JobMatchRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenerationService {

    private final JobMatchRepository matchRepository;
    private final JobRepository jobRepository;
    private final GenerationContextBuilder contextBuilder;
    private final LlmService llmService;
    private final GeneratedDocumentRepository repository;
    private final de.sellfish.profile.ProfileRepository profileRepository;

    public GenerationService(
            JobMatchRepository matchRepository,
            JobRepository jobRepository,
            GenerationContextBuilder contextBuilder,
            LlmService llmService,
            GeneratedDocumentRepository repository,
            de.sellfish.profile.ProfileRepository profileRepository) {
        this.matchRepository = matchRepository;
        this.jobRepository = jobRepository;
        this.contextBuilder = contextBuilder;
        this.llmService = llmService;
        this.repository = repository;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public GeneratedDocument generate(UUID userId, UUID jobMatchId, GenerationType type) {
        JobMatch match = matchRepository
                .findById(jobMatchId)
                .filter(m -> m.getUserId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Match not found"));
        Job job = jobRepository.findById(match.getJobId()).orElseThrow(() -> ApiException.notFound("Job not found"));

        // Guard: refuse to generate from an empty profile — otherwise the model
        // produces a useless "I can't write this" refusal and the user ships it.
        de.sellfish.profile.UserProfile profile =
                profileRepository.findByUserId(userId).orElse(null);
        boolean profileEmpty = profile == null || (isBlank(profile.getHeadline()) && isBlank(profile.getSummary()));
        if (profileEmpty) {
            throw ApiException.badRequest(
                    "Complete your profile (headline or summary) before generating application documents.");
        }

        String context = contextBuilder.build(userId, job);
        // max_tokens grosszuegig: opencode-go-Modelle (deepseek/glm/...) sind Reasoning-Modelle
        // und verbrauchen einen Grossteil des Budgets im reasoning_content. Bei nur 3000 frass
        // das Reasoning mit grossem RAG-Kontext (Job-Beschreibung + CV) das ganze Budget auf
        // -> leerer content. 8000 laesst genug Platz fuer Reasoning + eigentliches Anschreiben.
        ChatResult result = llmService.chat(
                userId,
                new ChatRequest(
                        List.of(ChatMessage.system(PromptTemplates.system(type)), ChatMessage.user(context)),
                        0.5,
                        8000));

        int nextVersion = repository
                .findFirstByUserIdAndJobMatchIdAndTypeOrderByVersionDesc(userId, jobMatchId, type)
                .map(d -> d.getVersion() + 1)
                .orElse(1);

        GeneratedDocument doc = new GeneratedDocument(userId, jobMatchId, type);
        doc.setContent(result.content());
        doc.setModel(result.model());
        doc.setPromptVersion(PromptTemplates.VERSION);
        doc.setVersion(nextVersion);
        return repository.save(doc);
    }

    @Transactional(readOnly = true)
    public List<GeneratedDocument> list(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<GeneratedDocument> listForMatch(UUID userId, UUID jobMatchId) {
        return repository.findByUserIdAndJobMatchIdOrderByCreatedAtDesc(userId, jobMatchId);
    }

    @Transactional(readOnly = true)
    public GeneratedDocument get(UUID userId, UUID id) {
        return owned(userId, id);
    }

    @Transactional
    public GeneratedDocument updateContent(UUID userId, UUID id, String content) {
        GeneratedDocument doc = owned(userId, id);
        doc.setContent(content);
        return repository.save(doc);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        repository.delete(owned(userId, id));
    }

    private GeneratedDocument owned(UUID userId, UUID id) {
        GeneratedDocument doc = repository.findById(id).orElseThrow(() -> ApiException.notFound("Document not found"));
        if (!doc.getUserId().equals(userId)) {
            throw ApiException.notFound("Document not found");
        }
        return doc;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
