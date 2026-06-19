package de.sellfish.jobs.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import de.sellfish.support.AbstractPostgresIT;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class VectorStoreIT extends AbstractPostgresIT {

    @Autowired
    VectorStore vectorStore;

    @Autowired
    JdbcTemplate jdbc;

    private void seedUser(UUID userId) {
        jdbc.update("INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)", userId, userId + "@x.com", "h");
    }

    private void seedJob(UUID jobId) {
        jdbc.update(
                "INSERT INTO jobs (id, source_code, fingerprint, title) VALUES (?, ?, ?, ?)",
                jobId,
                "TEST",
                jobId.toString(),
                "Dev");
    }

    @Test
    void upsertAndReadProfileEmbedding() {
        UUID userId = UUID.randomUUID();
        seedUser(userId);
        assertThat(vectorStore.hasProfileEmbedding(userId)).isFalse();
        vectorStore.upsertProfileEmbedding(userId, new float[] {0.1f, 0.2f, 0.3f}, "test");
        assertThat(vectorStore.hasProfileEmbedding(userId)).isTrue();
        assertThat(vectorStore.getProfileEmbedding(userId)).containsExactly(0.1f, 0.2f, 0.3f);
    }

    @Test
    void upsertIsIdempotent() {
        UUID jobId = UUID.randomUUID();
        seedJob(jobId);
        vectorStore.upsertJobEmbedding(jobId, new float[] {1.0f, 0.0f, 0.0f}, "m");
        vectorStore.upsertJobEmbedding(jobId, new float[] {0.5f, 0.5f, 0.5f}, "m");
        assertThat(vectorStore.getJobEmbedding(jobId)).containsExactly(0.5f, 0.5f, 0.5f);
    }

    @Test
    void similarityAndRanking() {
        UUID userId = UUID.randomUUID();
        UUID jobA = UUID.randomUUID();
        UUID jobB = UUID.randomUUID();
        seedUser(userId);
        seedJob(jobA);
        seedJob(jobB);
        vectorStore.upsertProfileEmbedding(userId, new float[] {1f, 0f, 0f}, "m");
        vectorStore.upsertJobEmbedding(jobA, new float[] {1f, 0f, 0f}, "m"); // identical -> high
        vectorStore.upsertJobEmbedding(jobB, new float[] {0f, 1f, 0f}, "m"); // orthogonal -> low

        assertThat(vectorStore.similarity(userId, jobA)).isGreaterThan(vectorStore.similarity(userId, jobB));
        List<VectorStore.SimilarJob> ranked = vectorStore.similarJobsForUser(userId, 10);
        assertThat(ranked).isNotEmpty();
        assertThat(ranked.get(0).jobId()).isEqualTo(jobA);
    }

    @Test
    void similarityZeroWhenEmbeddingMissing() {
        assertThat(vectorStore.similarity(UUID.randomUUID(), UUID.randomUUID())).isEqualTo(0.0);
        assertThat(vectorStore.getProfileEmbedding(UUID.randomUUID())).isEmpty();
    }
}
