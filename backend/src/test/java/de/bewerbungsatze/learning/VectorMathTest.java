package de.bewerbungsatze.learning;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class VectorMathTest {

    @Test
    void centroidAveragesComponentwise() {
        float[] c = VectorMath.centroid(List.of(new float[]{0, 2}, new float[]{2, 4}));
        assertThat(c).containsExactly(1f, 3f);
    }

    @Test
    void centroidOfEmptyIsEmpty() {
        assertThat(VectorMath.centroid(List.of())).isEmpty();
    }

    @Test
    void normalizeYieldsUnitLength() {
        float[] n = VectorMath.normalize(new float[]{3, 4});
        double len = Math.sqrt(n[0] * n[0] + n[1] * n[1]);
        assertThat(len).isCloseTo(1.0, within(1e-6));
    }

    @Test
    void blendMovesTowardSecondVectorAndNormalizes() {
        float[] a = {1, 0};
        float[] b = {0, 1};
        float[] blended = VectorMath.blend(a, b, 0.5);
        // Gleichgewichtung -> Richtung 45°, normalisiert
        assertThat(blended[0]).isCloseTo(blended[1], within(1e-6f));
        double len = Math.sqrt(blended[0] * blended[0] + blended[1] * blended[1]);
        assertThat(len).isCloseTo(1.0, within(1e-6));
    }

    @Test
    void mismatchedDimensionsRejected() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> VectorMath.blend(new float[]{1}, new float[]{1, 2}, 0.5));
    }
}
