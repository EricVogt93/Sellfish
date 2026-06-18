package de.bewerbungsatze.profile;

/**
 * Request-/Response-DTOs für Profil & Präferenzen.
 */
public final class ProfileDtos {

    private ProfileDtos() {
    }

    public record ProfileRequest(
            String headline,
            String summary,
            String location,
            Boolean willingnessToRelocate,
            Integer salaryMin,
            String remotePref,
            String availability,
            String meta) {
    }

    public record ProfileResponse(
            String headline,
            String summary,
            String location,
            boolean willingnessToRelocate,
            Integer salaryMin,
            String remotePref,
            String availability,
            String meta) {

        public static ProfileResponse from(UserProfile p) {
            return new ProfileResponse(p.getHeadline(), p.getSummary(), p.getLocation(),
                    p.isWillingnessToRelocate(), p.getSalaryMin(), p.getRemotePref(),
                    p.getAvailability(), p.getMeta());
        }
    }

    public record PreferencesRequest(
            String[] desiredTitles,
            String[] industries,
            String companySize,
            String[] contractTypes,
            String[] excludedCompanies,
            String[] keywords,
            String hardFilters,
            String softWeights,
            String[] preferredCountries) {
    }

    public record PreferencesResponse(
            String[] desiredTitles,
            String[] industries,
            String companySize,
            String[] contractTypes,
            String[] excludedCompanies,
            String[] keywords,
            String hardFilters,
            String softWeights,
            String[] preferredCountries) {

        public static PreferencesResponse from(UserPreferences p) {
            return new PreferencesResponse(p.getDesiredTitles(), p.getIndustries(), p.getCompanySize(),
                    p.getContractTypes(), p.getExcludedCompanies(), p.getKeywords(),
                    p.getHardFilters(), p.getSoftWeights(), p.getPreferredCountries());
        }
    }
}
