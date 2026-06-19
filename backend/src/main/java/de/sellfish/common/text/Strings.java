package de.sellfish.common.text;

/** Shared string helpers — eliminates duplicated nz/truncate across 10+ files. */
public final class Strings {

    private Strings() {}

    /** Null-safe: returns empty string for null, otherwise the string itself. */
    public static String nz(String s) {
        return s == null ? "" : s;
    }

    /** Null-safe: returns "—" for null/blank, otherwise the string. */
    public static String nzOrDash(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    /** Truncate to maxLen, appending "..." if truncated. */
    public static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}
