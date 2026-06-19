package de.sellfish.jobs.adapter.source;

import java.util.Map;
import java.util.Set;

/**
 * Maps job source codes to the countries they cover.
 * "REMOTE" means worldwide remote (no country tie).
 */
public final class SourceCountries {

    private static final Set<String> REMOTE = Set.of("REMOTE");

    private static final Map<String, Set<String>> MAP = Map.<String, Set<String>>ofEntries(
            Map.entry("BA", Set.of("DE")),
            Map.entry("ADZUNA", Set.of("GB", "US", "DE", "FR", "NL", "IT", "ES", "PL", "AT", "CH")),
            Map.entry("4SCOTTY", Set.of("DE")),
            Map.entry("ITTALENTS", Set.of("DE", "AT", "CH")),
            Map.entry("HONEYPOT", Set.of("DE", "NL", "AT")),
            Map.entry("REMOTIVE", REMOTE),
            Map.entry("REMOTEOK", REMOTE),
            Map.entry("ARBEITNOW", Set.of("DE", "GB", "NL", "PL", "RO", "CZ", "HU", "AT", "CH", "FR")),
            Map.entry("HIMALAYAS", REMOTE),
            Map.entry("JOBICY", REMOTE),
            Map.entry("WORKINGNOMADS", REMOTE),
            Map.entry(
                    "EURREMOTE",
                    Set.of(
                            "DE", "GB", "FR", "NL", "IT", "ES", "PT", "AT", "CH", "SE", "DK", "NO", "FI", "PL", "CZ",
                            "IE", "BE", "LU")),
            Map.entry("REMOTECO", REMOTE),
            Map.entry("JUSTRMOTE", REMOTE),
            Map.entry("JOBSPRESSO", REMOTE),
            Map.entry("NODESK", REMOTE),
            Map.entry("WWREMOTE", REMOTE),
            Map.entry("THEMUSE", Set.of("US", "CA", "GB", "DE", "FR", "NL")),
            Map.entry("USAJOBS", Set.of("US")),
            Map.entry("REED", Set.of("GB")),
            Map.entry("JOOBLE", Set.of("DE", "GB", "US", "FR", "NL", "IT", "ES", "PL", "AT", "CH")),
            Map.entry("FINDWORK", REMOTE),
            Map.entry("CAREERJET", Set.of("DE", "GB", "US", "FR", "NL", "IT", "ES", "AT", "CH", "PL", "CZ")),
            Map.entry("ZIPRECRUITER", Set.of("US")),
            Map.entry("GREENHOUSE", Set.of("US", "DE", "GB", "CA", "NL", "FR", "AT", "CH")),
            Map.entry("LEVER", Set.of("US", "DE", "GB", "CA", "NL", "FR", "AT", "CH")),
            Map.entry("RECRUITEE", Set.of("DE", "NL", "GB", "FR", "AT", "CH")),
            Map.entry("ASHBY", Set.of("US", "DE", "GB", "CA", "NL")),
            Map.entry("SMARTRECRUITERS", Set.of("DE", "US", "GB", "NL", "FR", "ES", "IT", "AT", "CH")),
            Map.entry("WORKABLE", Set.of("US", "DE", "GB", "NL", "FR", "CA", "AU", "NZ")),
            Map.entry("SCRAPER", Set.of("DE", "GB", "US", "FR", "NL", "IT", "ES", "AT", "CH")),
            Map.entry("LLM_WEB", Set.of("DE", "GB", "US", "FR", "NL", "IT", "ES", "AT", "CH")));

    private SourceCountries() {}

    public static Set<String> countriesFor(String sourceCode) {
        return MAP.getOrDefault(sourceCode, REMOTE);
    }

    public static boolean isRemote(String sourceCode) {
        Set<String> c = countriesFor(sourceCode);
        return c.size() == 1 && c.contains("REMOTE");
    }

    public static java.util.List<CountryGroup> allGroups() {
        return java.util.List.of(
                new CountryGroup("DE", "Germany", "DE", true),
                new CountryGroup("AT", "Austria", "DE", false),
                new CountryGroup("CH", "Switzerland", "DE", false),
                new CountryGroup("GB", "UK", "GB", false),
                new CountryGroup("US", "USA", "US", false),
                new CountryGroup("CA", "Kanada", "CA", false),
                new CountryGroup("FR", "France", "FR", false),
                new CountryGroup("NL", "Netherlands", "NL", false),
                new CountryGroup("IT", "Italy", "IT", false),
                new CountryGroup("ES", "Spain", "ES", false),
                new CountryGroup("PL", "Poland", "PL", false),
                new CountryGroup("CZ", "Czechia", "CZ", false),
                new CountryGroup("SE", "Sweden", "SE", false),
                new CountryGroup("DK", "Denmark", "DK", false),
                new CountryGroup("NO", "Norway", "NO", false),
                new CountryGroup("FI", "Finland", "FI", false),
                new CountryGroup("IE", "Ireland", "IE", false),
                new CountryGroup("AU", "Australia", "AU", false),
                new CountryGroup("REMOTE", "Worldwide Remote", "REMOTE", true));
    }

    public record CountryGroup(String code, String label, String flag, boolean remote) {}

    public static java.util.List<String> availableCountries() {
        return allGroups().stream().map(CountryGroup::code).toList();
    }
}
