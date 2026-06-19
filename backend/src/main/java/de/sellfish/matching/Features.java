package de.sellfish.matching;

import java.util.LinkedHashMap;
import java.util.Map;

public record Features(
        double semantic,
        double title,
        double keyword,
        double location,
        double recency,
        double remote,
        double skillOverlap,
        double aiRelevance) {

    public Map<String, Double> asMap() {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put("semantic", round(semantic));
        m.put("title", round(title));
        m.put("keyword", round(keyword));
        m.put("location", round(location));
        m.put("recency", round(recency));
        m.put("remote", round(remote));
        m.put("skillOverlap", round(skillOverlap));
        m.put("aiRelevance", round(aiRelevance));
        return m;
    }

    public double[] toArray() {
        return new double[] {semantic, title, keyword, location, recency, remote, skillOverlap, aiRelevance};
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
