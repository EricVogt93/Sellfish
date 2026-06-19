package de.sellfish.enterprise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.sellfish.enterprise.LicenseService.LicenseStatus;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LicenseServiceTest {

    @Mock
    LicenseRepository licenseRepository;

    @Mock
    EnterpriseProperties properties;

    @InjectMocks
    LicenseService service;

    @BeforeEach
    void noStoredLicense() {
        when(properties.enabled()).thenReturn(false);
        when(licenseRepository.findTopByOrderByCreatedAtDesc()).thenReturn(Optional.empty());
    }

    @Test
    void featureEnabledByConfig() {
        when(properties.enabled()).thenReturn(true);
        when(properties.features()).thenReturn(new EnterpriseProperties.Features(true, false, false, false, false));
        assertThat(service.isEnterpriseFeatureEnabled("sso")).isTrue();
        assertThat(service.isEnterpriseFeatureEnabled("multi-tenant")).isFalse();
    }

    @Test
    void featureDisabledWhenEnterpriseOff() {
        when(properties.enabled()).thenReturn(false);
        when(properties.features()).thenReturn(new EnterpriseProperties.Features(true, true, true, true, true));
        assertThat(service.isEnterpriseFeatureEnabled("sso")).isFalse();
    }

    @Test
    void unknownFeatureAlwaysFalse() {
        when(properties.enabled()).thenReturn(true);
        when(properties.features()).thenReturn(new EnterpriseProperties.Features(true, true, true, true, true));
        assertThat(service.isEnterpriseFeatureEnabled("unknown")).isFalse();
    }

    @Test
    void statusNoLicenseWhenInvalid() {
        LicenseStatus status = service.getStatus();
        assertThat(status.valid()).isFalse();
    }

    @Test
    void uploadInvalidThrows() {
        // validator constructed with empty public key bytes -> any input invalid
        when(properties.publicKey()).thenReturn(java.util.Base64.getEncoder().encodeToString(new byte[0]));
        assertThatThrownBy(() -> service.uploadLicense("garbage::AA")).isInstanceOf(IllegalArgumentException.class);
        verify(licenseRepository, never()).save(any());
    }
}
