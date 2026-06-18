package de.sellfish.matching;

import de.sellfish.profile.UserPreferences;
import de.sellfish.profile.UserProfile;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record MatchContext(
        List<String> desiredTitles,
        List<String> keywords,
        String location,
        String remotePref,
        Set<String> excludedCompanies,
        String profileText) {

    public static MatchContext from(UserProfile profile, UserPreferences prefs) {
        return from(profile, prefs, "");
    }

    public static MatchContext from(UserProfile profile, UserPreferences prefs, String cvSkills) {
        List<String> titles = prefs == null ? List.of() : asList(prefs.getDesiredTitles());
        List<String> keywords = prefs == null ? List.of() : asList(prefs.getKeywords());
        Set<String> excluded = prefs == null ? Set.of()
                : Stream.of(nullSafe(prefs.getExcludedCompanies()))
                .map(s -> s.toLowerCase(Locale.ROOT).trim())
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
        String location = profile == null ? null : profile.getLocation();
        String remotePref = profile == null ? "ANY" : profile.getRemotePref();
        StringBuilder pt = new StringBuilder();
        if (profile != null) {
            if (profile.getHeadline() != null) pt.append(profile.getHeadline()).append('\n');
            if (profile.getSummary() != null) pt.append(profile.getSummary()).append('\n');
        }
        if (cvSkills != null && !cvSkills.isBlank()) pt.append(cvSkills);
        return new MatchContext(titles, keywords, location, remotePref, excluded, pt.toString());
    }

    private static List<String> asList(String[] arr) {
        return arr == null ? List.of() : Stream.of(arr).filter(s -> s != null && !s.isBlank()).toList();
    }

    private static String[] nullSafe(String[] arr) {
        return arr == null ? new String[0] : arr;
    }
}
