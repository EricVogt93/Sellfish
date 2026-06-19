package de.sellfish.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.sellfish.profile.ProfileDtos.PreferencesRequest;
import de.sellfish.profile.ProfileDtos.ProfileRequest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfileServiceTest {

    @Mock
    ProfileRepository profileRepository;

    @Mock
    PreferencesRepository preferencesRepository;

    @InjectMocks
    ProfileService service;

    @Test
    void getOrCreateReturnsExisting() {
        UUID userId = UUID.randomUUID();
        UserProfile existing = new UserProfile(userId);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        assertThat(service.getOrCreateProfile(userId)).isSameAs(existing);
        verify(profileRepository, never()).save(any());
    }

    @Test
    void getOrCreateCreatesWhenMissing() {
        UUID userId = UUID.randomUUID();
        UserProfile created = new UserProfile(userId);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(profileRepository.save(any())).thenReturn(created);
        assertThat(service.getOrCreateProfile(userId)).isSameAs(created);
    }

    @Test
    void updateProfileAppliesAllFields() {
        UUID userId = UUID.randomUUID();
        UserProfile p = new UserProfile(userId);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(p));
        when(profileRepository.save(p)).thenReturn(p);

        UserProfile result = service.updateProfile(
                userId, new ProfileRequest("Dev", "sum", "Berlin", true, 90000, "REMOTE", "2 weeks", "meta"));

        assertThat(result.getHeadline()).isEqualTo("Dev");
        assertThat(result.getLocation()).isEqualTo("Berlin");
        assertThat(result.getSalaryMin()).isEqualTo(90000);
        assertThat(result.getRemotePref()).isEqualTo("REMOTE");
    }

    @Test
    void updatePreferencesAppliesNonNullFields() {
        UUID userId = UUID.randomUUID();
        UserPreferences pref = new UserPreferences(userId);
        when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.of(pref));
        when(preferencesRepository.save(pref)).thenReturn(pref);

        service.updatePreferences(
                userId,
                new PreferencesRequest(new String[] {"Backend"}, null, null, null, null, null, null, null, null));
        assertThat(pref.getDesiredTitles()).containsExactly("Backend");
    }

    @Test
    void savePersistsProfileAndPreferences() {
        UserProfile p = new UserProfile(UUID.randomUUID());
        service.save(p);
        verify(profileRepository).save(p);
        UserPreferences pref = new UserPreferences(UUID.randomUUID());
        service.save(pref);
        verify(preferencesRepository).save(pref);
    }
}
