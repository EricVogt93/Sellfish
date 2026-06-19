package de.sellfish.jobs;

import de.sellfish.ai.LlmService;
import de.sellfish.jobs.adapter.persistence.VectorStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Berechnet und speichert Embeddings für Jobs und Profile.
 *
 * <p>Für lange Job-Texte (über CHUNK_CHARS) werden Chunks gebildet, einzeln embedded
 * und per Mean-Pooling zu einem Vektor fusioniert. So bleibt der volle Textinhalt
 * erhalten, without das Per-Slot-Token-Limit (llamacpp --ubatch-size 512) zu sprengen.
 */
@Service
public class JobEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(JobEmbeddingService.class);
    private static final int CHUNK_CHARS = 1200;

    private final LlmService llmService;
    private final VectorStore vectorStore;
    private final int expectedDimension;

    public JobEmbeddingService(
            LlmService llmService,
            VectorStore vectorStore,
            @Value("${app.embedding.dimension:768}") int expectedDimension) {
        this.llmService = llmService;
        this.vectorStore = vectorStore;
        this.expectedDimension = expectedDimension;
    }

    public boolean embedJob(Job job) {
        String text = jobText(job);
        try {
            float[] vector = embedChunked(job.getId(), text);
            if (vector == null || !dimensionOk(vector, "Job " + job.getId())) {
                return false;
            }
            vectorStore.upsertJobEmbedding(job.getId(), vector, "embedding");
            return true;
        } catch (RuntimeException e) {
            log.warn("Job embedding failed for {} ({} chars): {}", job.getId(), text.length(), e.getMessage());
            return false;
        }
    }

    public boolean embedProfile(UUID userId, String profileText) {
        try {
            float[] vector = llmService.embed(userId, truncateSingle(profileText));
            if (!dimensionOk(vector, "Profil " + userId)) {
                return false;
            }
            vectorStore.upsertProfileEmbedding(userId, vector, "embedding");
            return true;
        } catch (RuntimeException e) {
            log.debug("Profil-Embedding skipped for {}: {}", userId, e.getMessage());
            return false;
        }
    }

    private float[] embedChunked(UUID jobId, String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        if (text.length() <= CHUNK_CHARS) {
            return llmService.embed((UUID) null, text);
        }
        List<String> chunks = chunk(text, CHUNK_CHARS);
        List<float[]> vectors = new ArrayList<>();
        for (String chunk : chunks) {
            try {
                float[] v = llmService.embed((UUID) null, chunk);
                if (v != null && v.length == expectedDimension) {
                    vectors.add(v);
                }
            } catch (RuntimeException e) {
                log.warn("Chunk embedding failed for Job {}: {}", jobId, e.getMessage());
            }
        }
        if (vectors.isEmpty()) {
            return llmService.embed((UUID) null, truncateSingle(text));
        }
        if (vectors.size() == 1) {
            return vectors.get(0);
        }
        return meanPool(vectors);
    }

    /** Teilt Text in überlappungsfreie Chunks, möglichst an Satzgrenzen. */
    static List<String> chunk(String text, int maxLen) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxLen, text.length());
            if (end < text.length()) {
                int cut = text.lastIndexOf('.', end);
                if (cut > start + maxLen / 2) {
                    end = cut + 1;
                } else {
                    cut = text.lastIndexOf('\n', end);
                    if (cut > start + maxLen / 2) end = cut + 1;
                    else {
                        cut = text.lastIndexOf(' ', end);
                        if (cut > start + maxLen / 2) end = cut;
                    }
                }
            }
            chunks.add(text.substring(start, end).trim());
            start = end;
        }
        return chunks;
    }

    static float[] meanPool(List<float[]> vectors) {
        int dim = vectors.get(0).length;
        double[] sum = new double[dim];
        for (float[] v : vectors) {
            for (int i = 0; i < dim; i++) {
                sum[i] += v[i];
            }
        }
        float[] out = new float[dim];
        double n = vectors.size();
        for (int i = 0; i < dim; i++) {
            out[i] = (float) (sum[i] / n);
        }
        return normalize(out);
    }

    static float[] normalize(float[] v) {
        double norm = 0;
        for (float x : v) norm += x * x;
        norm = Math.sqrt(norm);
        if (norm == 0) return v.clone();
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) out[i] = (float) (v[i] / norm);
        return out;
    }

    private boolean dimensionOk(float[] vector, String what) {
        if (vector.length != expectedDimension) {
            log.warn(
                    "Embedding for {} discarded: Dimension {} ≠ schema dimension {}. "
                            + "Setze EMBEDDING_DIM passend zum Modell (vor dem ersten DB-Start).",
                    what,
                    vector.length,
                    expectedDimension);
            return false;
        }
        return true;
    }

    static String jobText(Job job) {
        StringBuilder sb = new StringBuilder();
        sb.append(job.getTitle());
        if (job.getCompany() != null) sb.append('\n').append(job.getCompany());
        if (job.getLocation() != null) sb.append('\n').append(job.getLocation());
        if (job.getDescription() != null) sb.append('\n').append(job.getDescription());
        return sb.toString();
    }

    private static String truncateSingle(String s) {
        if (s == null) return "";
        return s.length() > CHUNK_CHARS ? s.substring(0, CHUNK_CHARS) : s;
    }
}
