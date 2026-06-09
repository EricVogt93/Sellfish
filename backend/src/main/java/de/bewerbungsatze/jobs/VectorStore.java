package de.bewerbungsatze.jobs;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Zugriff auf die pgvector-Embeddings (Profil & Jobs) inkl. Cosine-Ähnlichkeitssuche.
 */
@Component
public class VectorStore {

    private final JdbcTemplate jdbc;

    public VectorStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void upsertJobEmbedding(UUID jobId, float[] embedding, String model) {
        jdbc.update("""
                INSERT INTO job_embedding (job_id, embedding, model, generated_at)
                VALUES (?, ?::vector, ?, now())
                ON CONFLICT (job_id) DO UPDATE
                  SET embedding = EXCLUDED.embedding, model = EXCLUDED.model, generated_at = now()
                """, jobId, toLiteral(embedding), model);
    }

    public void upsertProfileEmbedding(UUID userId, float[] embedding, String model) {
        jdbc.update("""
                INSERT INTO profile_embedding (user_id, embedding, model, generated_at)
                VALUES (?, ?::vector, ?, now())
                ON CONFLICT (user_id) DO UPDATE
                  SET embedding = EXCLUDED.embedding, model = EXCLUDED.model, generated_at = now()
                """, userId, toLiteral(embedding), model);
    }

    public boolean hasProfileEmbedding(UUID userId) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM profile_embedding WHERE user_id = ?", Integer.class, userId);
        return count != null && count > 0;
    }

    /**
     * Liefert die ähnlichsten Jobs (Cosine) zum Profil-Embedding des Nutzers.
     */
    public List<SimilarJob> similarJobsForUser(UUID userId, int limit) {
        return jdbc.query("""
                SELECT je.job_id, 1 - (je.embedding <=> pe.embedding) AS similarity
                FROM job_embedding je
                CROSS JOIN profile_embedding pe
                WHERE pe.user_id = ?
                ORDER BY je.embedding <=> pe.embedding
                LIMIT ?
                """,
                (rs, rowNum) -> new SimilarJob(
                        rs.getObject("job_id", UUID.class), rs.getDouble("similarity")),
                userId, limit);
    }

    /**
     * Cosine-Ähnlichkeit zwischen Profil und einem konkreten Job (0, falls Embedding fehlt).
     */
    public double similarity(UUID userId, UUID jobId) {
        List<Double> result = jdbc.query("""
                SELECT 1 - (je.embedding <=> pe.embedding) AS similarity
                FROM job_embedding je
                CROSS JOIN profile_embedding pe
                WHERE pe.user_id = ? AND je.job_id = ?
                """,
                (rs, rowNum) -> rs.getDouble("similarity"), userId, jobId);
        return result.isEmpty() ? 0.0 : result.get(0);
    }

    static String toLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(embedding[i]);
        }
        return sb.append(']').toString();
    }

    public record SimilarJob(UUID jobId, double similarity) {
    }
}
