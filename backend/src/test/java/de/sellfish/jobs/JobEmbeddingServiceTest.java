package de.sellfish.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import org.junit.jupiter.api.Test;

class JobEmbeddingServiceTest {

    @Test
    void chunkShortTextReturnsSingleChunk() {
        assertThat(JobEmbeddingService.chunk("hello world", 1000)).containsExactly("hello world");
    }

    @Test
    void chunkBlankReturnsEmpty() {
        assertThat(JobEmbeddingService.chunk("   ", 1000)).containsExactly("");
    }

    @Test
    void chunkSplitsLongTextAtSentenceBoundary() {
        String text = "First sentence. ".repeat(200); // ~3000 chars, plenty of dots
        List<String> chunks = JobEmbeddingService.chunk(text, 500);
        assertThat(chunks.size()).isGreaterThan(1);
        assertThat(String.join("", chunks).replaceAll("\\s", ""))
                .hasSize(text.replaceAll("\\s", "").length());
    }

    @Test
    void meanPoolAveragesAndNormalizes() {
        float[] a = {3f, 4f};
        float[] b = {0f, 0f};
        // mean = [1.5, 2.0], norm = sqrt(1.5^2 + 2^2) = 2.5 -> [0.6, 0.8]
        float[] result = JobEmbeddingService.meanPool(List.of(a, b));
        assertThat(result[0]).isCloseTo(0.6f, within(0.001f));
        assertThat(result[1]).isCloseTo(0.8f, within(0.001f));
    }

    @Test
    void normalizeUnitVectorStaysUnit() {
        float[] v = {0f, 5f};
        float[] n = JobEmbeddingService.normalize(v);
        assertThat(n).containsExactly(0f, 1f);
    }

    @Test
    void normalizeZeroVectorReturnedAsIs() {
        float[] v = {0f, 0f};
        assertThat(JobEmbeddingService.normalize(v)).containsExactly(0f, 0f);
    }

    @Test
    void jobTextConcatenatesAllFields() {
        Job job = new Job();
        job.setTitle("Dev");
        job.setCompany("Acme");
        job.setLocation("Berlin");
        job.setDescription("Build stuff");
        assertThat(JobEmbeddingService.jobText(job)).contains("Dev", "Acme", "Berlin", "Build stuff");
    }

    @Test
    void jobTextHandlesNulls() {
        Job job = new Job();
        job.setTitle("Dev");
        assertThat(JobEmbeddingService.jobText(job)).isEqualTo("Dev");
    }
}
