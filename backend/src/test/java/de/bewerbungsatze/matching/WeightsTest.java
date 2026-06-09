package de.bewerbungsatze.matching;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class WeightsTest {

    @Test
    void weightedScoreIsNormalized() {
        Weights w = new Weights(1, 0, 0, 0, 0, 0);
        Features f = new Features(0.8, 0.1, 0.1, 0.1, 0.1, 0.1);
        // Nur semantic zählt, Gewichtssumme = 1 -> Ergebnis = semantic
        assertThat(w.weightedScore(f)).isCloseTo(0.8, within(1e-9));
    }

    @Test
    void weightedScoreCombinesFeatures() {
        Weights w = new Weights(0.5, 0.5, 0, 0, 0, 0);
        Features f = new Features(1.0, 0.0, 0, 0, 0, 0);
        assertThat(w.weightedScore(f)).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void zeroWeightsYieldZero() {
        Weights w = new Weights(0, 0, 0, 0, 0, 0);
        assertThat(w.weightedScore(new Features(1, 1, 1, 1, 1, 1))).isZero();
    }

    @Test
    void mapRoundTrip() {
        Weights w = Weights.defaults();
        assertThat(Weights.fromMap(w.asMap())).isEqualTo(w);
    }

    @Test
    void fromMapUsesDefaultsForMissingKeys() {
        Weights w = Weights.fromMap(Map.of("semantic", 0.9));
        assertThat(w.semantic()).isEqualTo(0.9);
        assertThat(w.title()).isEqualTo(0.20); // Default
    }
}
