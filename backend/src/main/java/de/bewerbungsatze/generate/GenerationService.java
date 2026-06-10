package de.bewerbungsatze.generate;

import de.bewerbungsatze.ai.LlmService;
import de.bewerbungsatze.ai.model.ChatMessage;
import de.bewerbungsatze.ai.model.ChatRequest;
import de.bewerbungsatze.ai.model.ChatResult;
import de.bewerbungsatze.common.error.ApiException;
import de.bewerbungsatze.jobs.Job;
import de.bewerbungsatze.jobs.JobRepository;
import de.bewerbungsatze.matching.JobMatch;
import de.bewerbungsatze.matching.JobMatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GenerationService {

    private final JobMatchRepository matchRepository;
    private final JobRepository jobRepository;
    private final GenerationContextBuilder contextBuilder;
    private final LlmService llmService;
    private final GeneratedDocumentRepository repository;

    public GenerationService(JobMatchRepository matchRepository,
                             JobRepository jobRepository,
                             GenerationContextBuilder contextBuilder,
                             LlmService llmService,
                             GeneratedDocumentRepository repository) {
        this.matchRepository = matchRepository;
        this.jobRepository = jobRepository;
        this.contextBuilder = contextBuilder;
        this.llmService = llmService;
        this.repository = repository;
    }

    @Transactional
    public GeneratedDocument generate(UUID userId, UUID jobMatchId, GenerationType type) {
        JobMatch match = matchRepository.findById(jobMatchId)
                .filter(m -> m.getUserId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Match nicht gefunden"));
        Job job = jobRepository.findById(match.getJobId())
                .orElseThrow(() -> ApiException.notFound("Stelle nicht gefunden"));

        String context = contextBuilder.build(userId, job);
        // max_tokens grosszuegig: opencode-go-Modelle (deepseek/glm/...) sind Reasoning-Modelle
        // und verbrauchen einen Grossteil des Budgets im reasoning_content. Bei nur 3000 frass
        // das Reasoning mit grossem RAG-Kontext (Job-Beschreibung + CV) das ganze Budget auf
        // -> leerer content. 8000 laesst genug Platz fuer Reasoning + eigentliches Anschreiben.
        ChatResult result = llmService.chat(userId, new ChatRequest(
                List.of(ChatMessage.system(PromptTemplates.system(type)), ChatMessage.user(context)),
                0.5, 8000));

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
        GeneratedDocument doc = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Dokument nicht gefunden"));
        if (!doc.getUserId().equals(userId)) {
            throw ApiException.notFound("Dokument nicht gefunden");
        }
        return doc;
    }
}
