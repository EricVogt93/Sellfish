package de.bewerbungsatze.matching;

import de.bewerbungsatze.profile.UserPreferences;
import de.bewerbungsatze.profile.UserProfile;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Aufbereiteter Nutzerkontext (Wünsche, Filter, Profil) für das Scoring.
 */
public record MatchContext(
        List<String> desiredTitles,
        List<String> keywords,
        String location,
        String remotePref,
        Set<String> excludedCompanies) {

    public static MatchContext from(UserProfile profile, UserPreferences prefs) {
        List<String> titles = prefs == null ? List.of() : asList(prefs.getDesiredTitles());
        List<String> keywords = prefs == null ? List.of() : asList(prefs.getKeywords());
        Set<String> excluded = prefs == null ? Set.of()
                : Stream.of(nullSafe(prefs.getExcludedCompanies()))
                .map(s -> s.toLowerCase(Locale.ROOT).trim())
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
        String location = profile == null ? null : profile.getLocation();
        String remotePref = profile == null ? "ANY" : profile.getRemotePref();
        return new MatchContext(titles, keywords, location, remotePref, excluded);
    }

    private static List<String> asList(String[] arr) {
        return arr == null ? List.of() : Stream.of(arr).filter(s -> s != null && !s.isBlank()).toList();
    }

    private static String[] nullSafe(String[] arr) {
        return arr == null ? new String[0] : arr;
    }
}
