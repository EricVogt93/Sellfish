package de.bewerbungsatze.profile;

import de.bewerbungsatze.common.security.CurrentUser;
import de.bewerbungsatze.profile.ProfileDtos.PreferencesRequest;
import de.bewerbungsatze.profile.ProfileDtos.PreferencesResponse;
import de.bewerbungsatze.profile.ProfileDtos.ProfileRequest;
import de.bewerbungsatze.profile.ProfileDtos.ProfileResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ProfileResponse getProfile() {
        return ProfileResponse.from(profileService.getOrCreateProfile(CurrentUser.id()));
    }

    @PutMapping
    public ProfileResponse updateProfile(@RequestBody ProfileRequest req) {
        return ProfileResponse.from(profileService.updateProfile(CurrentUser.id(), req));
    }

    @GetMapping("/preferences")
    public PreferencesResponse getPreferences() {
        return PreferencesResponse.from(profileService.getOrCreatePreferences(CurrentUser.id()));
    }

    @PutMapping("/preferences")
    public PreferencesResponse updatePreferences(@RequestBody PreferencesRequest req) {
        return PreferencesResponse.from(profileService.updatePreferences(CurrentUser.id(), req));
    }
}
