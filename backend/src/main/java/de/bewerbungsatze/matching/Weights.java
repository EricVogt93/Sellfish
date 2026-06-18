package de.bewerbungsatze.matching;

import java.util.LinkedHashMap;
import java.util.Map;

public record Weights(
        double semantic,
        double title,
        double keyword,
        double location,
        double recency,
        double remote,
        double skillOverlap,
        double aiRelevance) {

    public static Weights defaults() {
        return new Weights(0.30, 0.15, 0.10, 0.08, 0.08, 0.04, 0.10, 0.15);
    }

    public double weightedScore(Features f) {
        double sum = semantic + title + keyword + location + recency + remote + skillOverlap + aiRelevance;
        if (sum == 0) return 0;
        double total = semantic * f.semantic()
                + title * f.title()
                + keyword * f.keyword()
                + location * f.location()
                + recency * f.recency()
                + remote * f.remote()
                + skillOverlap * f.skillOverlap()
                + aiRelevance * f.aiRelevance();
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
        m.put("skillOverlap", skillOverlap);
        m.put("aiRelevance", aiRelevance);
        return m;
    }

    public static Weights fromMap(Map<String, ?> m) {
        return new Weights(
                d(m, "semantic", 0.30),
                d(m, "title", 0.15),
                d(m, "keyword", 0.10),
                d(m, "location", 0.08),
                d(m, "recency", 0.08),
                d(m, "remote", 0.04),
                d(m, "skillOverlap", 0.10),
                d(m, "aiRelevance", 0.15));
    }

    private static double d(Map<String, ?> m, String key, double def) {
        Object v = m.get(key);
        return v instanceof Number n ? n.doubleValue() : def;
    }
}
