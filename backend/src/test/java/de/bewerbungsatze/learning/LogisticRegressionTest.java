package de.bewerbungsatze.learning;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogisticRegressionTest {

    @Test
    void learnsSeparableData() {
        double[][] x = {
                {0.0}, {0.1}, {0.2}, {0.15},
                {0.85}, {0.9}, {1.0}, {0.95}
        };
        double[] y = {0, 0, 0, 0, 1, 1, 1, 1};

        LogisticRegression model = LogisticRegression.fit(x, y, 2000, 0.5, 0.0);

        assertThat(model.accuracy(x, y)).isEqualTo(1.0);
        assertThat(model.predictProba(new double[]{0.95}))
                .isGreaterThan(model.predictProba(new double[]{0.05}));
        assertThat(model.coefficients()[0]).isPositive();
    }

    @Test
    void emptyDataIsRejected() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> LogisticRegression.fit(new double[0][0], new double[0], 10, 0.1, 0.0));
    }
}
