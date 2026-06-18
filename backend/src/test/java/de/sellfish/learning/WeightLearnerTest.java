package de.sellfish.learning;

import de.sellfish.matching.Features;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeightLearnerTest {

    @Test
    void learnsThatSemanticDrivesPositiveFeedback() {
        List<Features> samples = new ArrayList<>();
        List<Double> labels = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            samples.add(new Features(0.85 + i * 0.01, 0.5, 0.5, 0.5, 0.5, 0.5, 0.0, 0.5));
            labels.add(1.0);
        }
        for (int i = 0; i < 5; i++) {
            samples.add(new Features(0.1 + i * 0.01, 0.5, 0.5, 0.5, 0.5, 0.5, 0.0, 0.5));
            labels.add(0.0);
        }
        double[] y = labels.stream().mapToDouble(Double::doubleValue).toArray();

        WeightLearner.LearnResult result = WeightLearner.learn(samples, y);

        assertThat(result.accuracy()).isGreaterThanOrEqualTo(0.9);
        assertThat(result.samples()).isEqualTo(10);
        var w = result.weights();
        assertThat(w.semantic()).isGreaterThan(w.title());
        assertThat(w.semantic()).isGreaterThan(w.keyword());
    }

    @Test
    void mismatchedSizesRejected() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> WeightLearner.learn(
                        List.of(new Features(0, 0, 0, 0, 0, 0, 0.0, 0.5)),
                        new double[]{1, 0}));
    }
}
