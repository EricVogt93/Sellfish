package de.sellfish.common.text;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Einfache Tokenisierung für das regelbasierte Matching.
 */
public final class TextTokens {

    private TextTokens() {}

    public static Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        String[] parts = text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{Nd}]+");
        Set<String> tokens = new LinkedHashSet<>();
        Arrays.stream(parts).filter(t -> t.length() > 1).forEach(tokens::add);
        return tokens;
    }

    public static boolean containsAny(String haystack, Set<String> needles) {
        if (haystack == null || needles.isEmpty()) {
            return false;
        }
        String lower = haystack.toLowerCase(Locale.ROOT);
        return needles.stream().anyMatch(lower::contains);
    }
}
