package de.sellfish.profile;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final PreferencesRepository preferencesRepository;

    public ProfileService(ProfileRepository profileRepository, PreferencesRepository preferencesRepository) {
        this.profileRepository = profileRepository;
        this.preferencesRepository = preferencesRepository;
    }

    @Transactional
    public UserProfile getOrCreateProfile(UUID userId) {
        return profileRepository.findByUserId(userId).orElseGet(() -> profileRepository.save(new UserProfile(userId)));
    }

    @Transactional
    public UserPreferences getOrCreatePreferences(UUID userId) {
        return preferencesRepository
                .findByUserId(userId)
                .orElseGet(() -> preferencesRepository.save(new UserPreferences(userId)));
    }

    @Transactional
    public UserProfile updateProfile(UUID userId, ProfileDtos.ProfileRequest req) {
        UserProfile p = getOrCreateProfile(userId);
        p.setHeadline(req.headline());
        p.setSummary(req.summary());
        p.setLocation(req.location());
        p.setWillingnessToRelocate(Boolean.TRUE.equals(req.willingnessToRelocate()));
        p.setSalaryMin(req.salaryMin());
        if (req.remotePref() != null) {
            p.setRemotePref(req.remotePref());
        }
        p.setAvailability(req.availability());
        if (req.meta() != null) {
            p.setMeta(req.meta());
        }
        return profileRepository.save(p);
    }

    @Transactional
    public UserPreferences updatePreferences(UUID userId, ProfileDtos.PreferencesRequest req) {
        UserPreferences pref = getOrCreatePreferences(userId);
        if (req.desiredTitles() != null) pref.setDesiredTitles(req.desiredTitles());
        if (req.industries() != null) pref.setIndustries(req.industries());
        pref.setCompanySize(req.companySize());
        if (req.contractTypes() != null) pref.setContractTypes(req.contractTypes());
        if (req.excludedCompanies() != null) pref.setExcludedCompanies(req.excludedCompanies());
        if (req.keywords() != null) pref.setKeywords(req.keywords());
        if (req.hardFilters() != null) pref.setHardFilters(req.hardFilters());
        if (req.softWeights() != null) pref.setSoftWeights(req.softWeights());
        if (req.preferredCountries() != null) pref.setPreferredCountries(req.preferredCountries());
        return preferencesRepository.save(pref);
    }

    @Transactional
    public UserProfile save(UserProfile profile) {
        return profileRepository.save(profile);
    }

    @Transactional
    public UserPreferences save(UserPreferences prefs) {
        return preferencesRepository.save(prefs);
    }
}
