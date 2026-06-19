package de.sellfish.jobs.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import de.sellfish.support.AbstractPostgresIT;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class VectorStoreIT extends AbstractPostgresIT {

    @Autowired
    VectorStore vectorStore;

    private final float[] vec(float... v) {
        return v;
    }

    @Test
    void upsertAndReadProfileEmbedding() {
        UUID userId = UUID.randomUUID();
        assertThat(vectorStore.hasProfileEmbedding(userId)).isFalse();
        vectorStore.upsertProfileEmbedding(userId, vec(0.1f, 0.2f, 0.3f), "test");
        assertThat(vectorStore.hasProfileEmbedding(userId)).isTrue();
        assertThat(vectorStore.getProfileEmbedding(userId)).containsExactly(0.1f, 0.2f, 0.3f);
    }

    @Test
    void upsertIsIdempotent() {
        UUID jobId = UUID.randomUUID();
        vectorStore.upsertJobEmbedding(jobId, vec(1.0f, 0.0f), "m");
        vectorStore.upsertJobEmbedding(jobId, vec(0.5f, 0.5f), "m");
        assertThat(vectorStore.getJobEmbedding(jobId)).containsExactly(0.5f, 0.5f);
    }

    @Test
    void similarityAndRanking() {
        UUID userId = UUID.randomUUID();
        UUID jobA = UUID.randomUUID();
        UUID jobB = UUID.randomUUID();
        vectorStore.upsertProfileEmbedding(userId, vec(1f, 0f), "m");
        vectorStore.upsertJobEmbedding(jobA, vec(1f, 0f), "m"); // identical -> high similarity
        vectorStore.upsertJobEmbedding(jobB, vec(0f, 1f), "m"); // orthogonal -> low similarity

        double simA = vectorStore.similarity(userId, jobA);
        double simB = vectorStore.similarity(userId, jobB);
        assertThat(simA).isGreaterThan(simB);

        List<VectorStore.SimilarJob> ranked = vectorStore.similarJobsForUser(userId, 10);
        assertThat(ranked).isNotEmpty();
        assertThat(ranked.get(0).jobId()).isEqualTo(jobA); // most similar first
    }

    @Test
    void similarityZeroWhenEmbeddingMissing() {
        assertThat(vectorStore.similarity(UUID.randomUUID(), UUID.randomUUID())).isEqualTo(0.0);
        assertThat(vectorStore.getProfileEmbedding(UUID.randomUUID())).isEmpty();
    }
}
