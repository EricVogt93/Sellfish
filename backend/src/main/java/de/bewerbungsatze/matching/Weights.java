package de.bewerbungsatze.matching;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gewichte der Score-Merkmale. Default-Werte dienen dem Kaltstart; das Self-Learning (M6)
 * überschreibt sie pro Nutzer.
 */
public record Weights(
        double semantic,
        double title,
        double keyword,
        double location,
        double recency,
        double remote) {

    public static Weights defaults() {
        return new Weights(0.40, 0.20, 0.15, 0.10, 0.10, 0.05);
    }

    public double weightedScore(Features f) {
        double sum = semantic + title + keyword + location + recency + remote;
        if (sum == 0) {
            return 0;
        }
        double total = semantic * f.semantic()
                + title * f.title()
                + keyword * f.keyword()
                + location * f.location()
                + recency * f.recency()
                + remote * f.remote();
        return total / sum;
    }

    public Map<String, Double> asMap() {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put("semantic", semantic);
        m.put("title", title);
        m.put("keyword", keyword);
        m.put("location", location);
        m.put("recency", recency);
        m.put("remote", remote);
        return m;
    }

    public static Weights fromMap(Map<String, ?> m) {
        return new Weights(
                d(m, "semantic", 0.40),
                d(m, "title", 0.20),
                d(m, "keyword", 0.15),
                d(m, "location", 0.10),
                d(m, "recency", 0.10),
                d(m, "remote", 0.05));
    }

    private static double d(Map<String, ?> m, String key, double def) {
        Object v = m.get(key);
        return v instanceof Number n ? n.doubleValue() : def;
    }
}
