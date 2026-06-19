package de.sellfish.jobs.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VectorStoreTest {

    @Test
    void toLiteralFormatsPgVectorSyntax() {
        assertThat(VectorStore.toLiteral(new float[] {0.1f, 0.2f, 0.3f})).isEqualTo("[0.1,0.2,0.3]");
        assertThat(VectorStore.toLiteral(new float[] {})).isEqualTo("[]");
    }

    @Test
    void parseLiteralParsesPgVectorText() {
        assertThat(VectorStore.parseLiteral("[0.1,0.2,0.3]")).containsExactly(0.1f, 0.2f, 0.3f);
    }

    @Test
    void parseLiteralHandlesEmptyAndShort() {
        assertThat(VectorStore.parseLiteral("")).isEmpty();
        assertThat(VectorStore.parseLiteral("{}")).isEmpty();
        assertThat(VectorStore.parseLiteral("[]")).isEmpty();
    }

    @Test
    void roundTripPreservesValues() {
        float[] original = {1.0f, 0.5f, -0.25f, 0.0f};
        float[] parsed = VectorStore.parseLiteral(VectorStore.toLiteral(original));
        assertThat(parsed).containsExactly(original);
    }
}
