package de.bewerbungsatze.learning;

import java.util.List;

/**
 * Reine Vektoroperationen für den Profil-Drift (Centroid-Verschiebung).
 */
public final class VectorMath {

    private VectorMath() {
    }

    public static float[] centroid(List<float[]> vectors) {
        if (vectors.isEmpty()) {
            return new float[0];
        }
        int dim = vectors.get(0).length;
        double[] sum = new double[dim];
        for (float[] v : vectors) {
            if (v.length != dim) {
                throw new IllegalArgumentException("Inkonsistente Vektordimensionen");
            }
            for (int i = 0; i < dim; i++) {
                sum[i] += v[i];
            }
        }
        float[] out = new float[dim];
        for (int i = 0; i < dim; i++) {
            out[i] = (float) (sum[i] / vectors.size());
        }
        return out;
    }

    /**
     * Gewichtete Mischung {@code alpha*a + (1-alpha)*b}, anschließend L2-normalisiert.
     */
    public static float[] blend(float[] a, float[] b, double alpha) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Dimensionen müssen übereinstimmen");
        }
        float[] out = new float[a.length];
        for (int i = 0; i < a.length; i++) {
            out[i] = (float) (alpha * a[i] + (1 - alpha) * b[i]);
        }
        return normalize(out);
    }

    public static float[] normalize(float[] v) {
        double norm = 0;
        for (float x : v) {
            norm += x * x;
        }
        norm = Math.sqrt(norm);
        if (norm == 0) {
            return v.clone();
        }
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) {
            out[i] = (float) (v[i] / norm);
        }
        return out;
    }
}
