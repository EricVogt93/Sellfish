package de.bewerbungsatze.matching;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Normierte Merkmalswerte (jeweils 0..1) eines Job-Kandidaten für einen Nutzer.
 */
public record Features(
        double semantic,
        double title,
        double keyword,
        double location,
        double recency,
        double remote) {

    public Map<String, Double> asMap() {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put("semantic", round(semantic));
        m.put("title", round(title));
        m.put("keyword", round(keyword));
        m.put("location", round(location));
        m.put("recency", round(recency));
        m.put("remote", round(remote));
        return m;
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
