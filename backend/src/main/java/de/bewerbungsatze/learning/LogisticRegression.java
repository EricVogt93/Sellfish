package de.bewerbungsatze.learning;

/**
 * Schlanke logistische Regression (Batch-Gradientenabstieg) mit L2-Regularisierung.
 * Bewusst abhängigkeitsfrei und deterministisch, damit das Self-Learning ohne GPU
 * und ohne externe ML-Bibliothek auskommt.
 */
public final class LogisticRegression {

    private final double[] coefficients; // ohne Intercept
    private final double intercept;

    private LogisticRegression(double[] coefficients, double intercept) {
        this.coefficients = coefficients;
        this.intercept = intercept;
    }

    public static LogisticRegression fit(double[][] x, double[] y, int iterations, double lr, double l2) {
        if (x.length == 0) {
            throw new IllegalArgumentException("Keine Trainingsdaten");
        }
        int n = x.length;
        int dim = x[0].length;
        double[] w = new double[dim];
        double b = 0.0;

        for (int it = 0; it < iterations; it++) {
            double[] gradW = new double[dim];
            double gradB = 0.0;
            for (int i = 0; i < n; i++) {
                double pred = sigmoid(dot(w, x[i]) + b);
                double error = pred - y[i];
                for (int j = 0; j < dim; j++) {
                    gradW[j] += error * x[i][j];
                }
                gradB += error;
            }
            for (int j = 0; j < dim; j++) {
                w[j] -= lr * (gradW[j] / n + l2 * w[j]);
            }
            b -= lr * (gradB / n);
        }
        return new LogisticRegression(w, b);
    }

    public double predictProba(double[] features) {
        return sigmoid(dot(coefficients, features) + intercept);
    }

    public double[] coefficients() {
        return coefficients.clone();
    }

    public double intercept() {
        return intercept;
    }

    /** Trainingsgenauigkeit bei Schwellwert 0.5. */
    public double accuracy(double[][] x, double[] y) {
        int correct = 0;
        for (int i = 0; i < x.length; i++) {
            double p = predictProba(x[i]) >= 0.5 ? 1.0 : 0.0;
            if (p == y[i]) {
                correct++;
            }
        }
        return x.length == 0 ? 0.0 : (double) correct / x.length;
    }

    private static double dot(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    private static double sigmoid(double z) {
        return 1.0 / (1.0 + Math.exp(-z));
    }
}
