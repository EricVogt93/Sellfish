package de.bewerbungsatze.jobs;
import de.bewerbungsatze.jobs.adapter.persistence.VectorStore;

import de.bewerbungsatze.ai.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Berechnet und speichert Embeddings für Jobs und Profile (Best-Effort: scheitert leise,
 * wenn kein Embedding-Provider konfiguriert ist oder die Dimension nicht passt).
 */
@Service
public class JobEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(JobEmbeddingService.class);
    private static final int MAX_CHARS = 8000;

    private final LlmService llmService;
    private final VectorStore vectorStore;

    public JobEmbeddingService(LlmService llmService, VectorStore vectorStore) {
        this.llmService = llmService;
        this.vectorStore = vectorStore;
    }

    public boolean embedJob(Job job) {
        String text = jobText(job);
        try {
            float[] vector = llmService.embed((UUID) null, text);
            vectorStore.upsertJobEmbedding(job.getId(), vector, "embedding");
            return true;
        } catch (RuntimeException e) {
            log.debug("Job-Embedding übersprungen für {}: {}", job.getId(), e.getMessage());
            return false;
        }
    }

    public boolean embedProfile(UUID userId, String profileText) {
        try {
            float[] vector = llmService.embed(userId, truncate(profileText));
            vectorStore.upsertProfileEmbedding(userId, vector, "embedding");
            return true;
        } catch (RuntimeException e) {
            log.debug("Profil-Embedding übersprungen für {}: {}", userId, e.getMessage());
            return false;
        }
    }

    static String jobText(Job job) {
        StringBuilder sb = new StringBuilder();
        sb.append(job.getTitle());
        if (job.getCompany() != null) {
            sb.append('\n').append(job.getCompany());
        }
        if (job.getLocation() != null) {
            sb.append('\n').append(job.getLocation());
        }
        if (job.getDescription() != null) {
            sb.append('\n').append(job.getDescription());
        }
        return truncate(sb.toString());
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > MAX_CHARS ? s.substring(0, MAX_CHARS) : s;
    }
}
