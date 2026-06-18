package de.bewerbungsatze.learning;

import de.bewerbungsatze.matching.Features;
import de.bewerbungsatze.matching.Weights;

import java.util.List;

/**
 * Lernt aus gelabelten Feature-Beispielen (positives/negatives Feedback) neue Score-Gewichte.
 */
public final class WeightLearner {

    private static final int ITERATIONS = 800;
    private static final double LEARNING_RATE = 0.3;
    private static final double L2 = 0.01;

    private WeightLearner() {
    }

    public static LearnResult learn(List<Features> samples, double[] labels) {
        if (samples.size() != labels.length) {
            throw new IllegalArgumentException("Sample-/Label-Anzahl unterschiedlich");
        }
        double[][] x = new double[samples.size()][];
        for (int i = 0; i < samples.size(); i++) {
            x[i] = toArray(samples.get(i));
        }
        LogisticRegression model = LogisticRegression.fit(x, labels, ITERATIONS, LEARNING_RATE, L2);
        double[] coef = model.coefficients();

        // Negative Koeffizienten auf 0 kappen; relative Größe bestimmt die Gewichtung.
        double[] w = new double[coef.length];
        double sum = 0;
        for (int i = 0; i < coef.length; i++) {
            w[i] = Math.max(0, coef[i]);
            sum += w[i];
        }
        Weights weights = sum == 0
                ? Weights.defaults()
                : new Weights(w[0], w[1], w[2], w[3], w[4], w[5], w[6], w[7]);

        return new LearnResult(weights, model.accuracy(x, labels), samples.size());
    }

    static double[] toArray(Features f) {
        return f.toArray();
    }

    public record LearnResult(Weights weights, double accuracy, int samples) {
    }
}
