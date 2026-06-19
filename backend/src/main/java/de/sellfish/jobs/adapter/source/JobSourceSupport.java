package de.sellfish.jobs.adapter.source;
import de.sellfish.jobs.port.JobSource;
import de.sellfish.jobs.port.JobQuery;
import de.sellfish.jobs.port.RawJob;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Common helpers for job source adapters (field access, date parsing).
 */
public final class JobSourceSupport {

    private JobSourceSupport() {
    }

    public static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull()) {
            return null;
        }
        String s = v.asText("").strip();
        return s.isEmpty() ? null : s;
    }

    /** Joins multiple string nodes into one non-null value (first non-empty wins). */
    public static String firstText(JsonNode node, String... fields) {
        for (String f : fields) {
            String s = text(node, f);
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    public static Instant parseIso(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (Exception ignored) {
            // try alternate formats below
        }
        try {
            return LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static Instant parseEpochSeconds(JsonNode node, String field) {
        JsonNode v = node.path(field);
        if (v.isNumber()) {
            return Instant.ofEpochSecond(v.asLong());
        }
        if (v.isTextual()) {
            String s = v.asText().strip();
            if (s.matches("\\d+")) {
                return Instant.ofEpochSecond(Long.parseLong(s));
            }
            return parseIso(s);
        }
        return null;
    }
}
