package de.bewerbungsatze.jobs;
import de.bewerbungsatze.jobs.adapter.persistence.VectorStore;

import de.bewerbungsatze.ai.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Berechnet und speichert Embeddings für Jobs und Profile (Best-Effort: scheitert leise,
 * wenn kein Embedding-Provider konfiguriert ist oder die Dimension nicht passt).
 */
@Service
public class JobEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(JobEmbeddingService.class);
    // Begrenzung der Embedding-Eingabe. Harte Grenze ist die physical batch size des
    // Embedding-Servers (llamacpp-embed: --ubatch-size 512) — der gesamte Embedding-Input
    // muss in EINEN ubatch passen, sonst 500 "input too large to process". 8000 Zeichen
    // (alter Wert) sprengten das bei langen Board-Beschreibungen -> Embedding scheiterte
    // still -> Job wurde nie Match-Kandidat (nur BA mit kurzen Texten kam durch).
    // 1200 Zeichen (~350-450 Tokens) liegen sicher unter 512.
    private static final int MAX_CHARS = 1200;

    private final LlmService llmService;
    private final VectorStore vectorStore;
    private final int expectedDimension;

    public JobEmbeddingService(LlmService llmService,
                               VectorStore vectorStore,
                               @Value("${app.embedding.dimension:768}") int expectedDimension) {
        this.llmService = llmService;
        this.vectorStore = vectorStore;
        this.expectedDimension = expectedDimension;
    }

    public boolean embedJob(Job job) {
        String text = jobText(job);
        try {
            float[] vector = llmService.embed((UUID) null, text);
            if (!dimensionOk(vector, "Job " + job.getId())) {
                return false;
            }
            vectorStore.upsertJobEmbedding(job.getId(), vector, "embedding");
            return true;
        } catch (RuntimeException e) {
            log.warn("Job-Embedding fehlgeschlagen für {} ({} Zeichen): {}",
                    job.getId(), text.length(), e.getMessage());
            return false;
        }
    }

    public boolean embedProfile(UUID userId, String profileText) {
        try {
            float[] vector = llmService.embed(userId, truncate(profileText));
            if (!dimensionOk(vector, "Profil " + userId)) {
                return false;
            }
            vectorStore.upsertProfileEmbedding(userId, vector, "embedding");
            return true;
        } catch (RuntimeException e) {
            log.debug("Profil-Embedding übersprungen für {}: {}", userId, e.getMessage());
            return false;
        }
    }

    /** Schützt vor stillen Insert-Fehlern, wenn das Modell eine andere Dimension liefert. */
    private boolean dimensionOk(float[] vector, String what) {
        if (vector.length != expectedDimension) {
            log.warn("Embedding für {} verworfen: Dimension {} ≠ Schema-Dimension {}. "
                            + "Setze EMBEDDING_DIM passend zum Modell (vor dem ersten DB-Start).",
                    what, vector.length, expectedDimension);
            return false;
        }
        return true;
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
