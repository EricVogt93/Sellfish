package de.bewerbungsatze.enterprise;

import de.bewerbungsatze.enterprise.LicenseValidator.LicensePayload;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Zentraler Dienst für Lizenz- und Feature-Toggle-Logik.
 *
 * <p>Prüft in dieser Reihenfolge:
 * <ol>
 *   <li>{@code app.enterprise.enabled = false} → nur statische Config</li>
 *   <li>{@code app.enterprise.enabled = true} → statische Config ODER gültige Lizenz</li>
 * </ol>
 *
 * <p>Feature ist aktiv, wenn es in der statischen Config {@code true} ist ODER
 * eine hochgeladene gültige Lizenz das Feature einschließt.
 */
@Service
public class LicenseService {

    private static final Logger log = LoggerFactory.getLogger(LicenseService.class);

    private final LicenseRepository licenseRepository;
    private final EnterpriseProperties properties;
    private volatile LicensePayload activePayload = LicensePayload.INVALID;

    public LicenseService(LicenseRepository licenseRepository, EnterpriseProperties properties) {
        this.licenseRepository = licenseRepository;
        this.properties = properties;
    }

    @PostConstruct
    void loadStoredLicense() {
        licenseRepository.findTopByOrderByCreatedAtDesc().ifPresent(entity -> {
            LicenseValidator validator = new LicenseValidator(properties.publicKey());
            LicensePayload payload = validator.validate(entity.getLicenseKey());
            if (payload.valid()) {
                activePayload = payload;
                log.info("Gespeicherte Lizenz geladen: sub={}, features={}", payload.subject(), payload.features());
            } else {
                log.warn("Gespeicherte Lizenz ist ungültig");
            }
        });
    }

    public LicensePayload uploadLicense(String licenseKey) {
        LicenseValidator validator = new LicenseValidator(properties.publicKey());
        LicensePayload payload = validator.validate(licenseKey);
        if (!payload.valid()) {
            throw new IllegalArgumentException("Lizenz-Key ungültig oder abgelaufen");
        }
        licenseRepository.deleteAll();
        LicenseEntity entity = new LicenseEntity(
                licenseKey, payload.subject(), payload.expires(),
                String.join(",", payload.features()));
        licenseRepository.save(entity);
        activePayload = payload;
        log.info("Lizenz aktiviert: sub={}, features={}", payload.subject(), payload.features());
        return payload;
    }

    public LicenseStatus getStatus() {
        LicensePayload p = activePayload;
        if (!p.valid()) {
            return LicenseStatus.noLicense(isEnterpriseEnabled());
        }
        return new LicenseStatus(true, p.subject(), p.expires(), p.features(), featuresFromConfig());
    }

    public boolean isEnterpriseFeatureEnabled(String feature) {
        return isFeatureEnabledByConfig(feature) || isFeatureEnabledByLicense(feature);
    }

    private boolean isFeatureEnabledByConfig(String feature) {
        if (!properties.enabled()) return false;
        EnterpriseProperties.Features f = properties.features();
        if (f == null) return false;
        return switch (feature) {
            case "sso" -> f.sso();
            case "multi-tenant" -> f.multiTenant();
            case "reports" -> f.reports();
            case "audit-log" -> f.auditLog();
            case "ha" -> f.ha();
            default -> false;
        };
    }

    private boolean isFeatureEnabledByLicense(String feature) {
        return activePayload.hasFeature(feature);
    }

    private boolean isEnterpriseEnabled() {
        return properties.enabled();
    }

    private Set<String> featuresFromConfig() {
        if (!properties.enabled() || properties.features() == null) return Set.of();
        Set<String> set = new java.util.LinkedHashSet<>();
        if (properties.features().sso()) set.add("sso");
        if (properties.features().multiTenant()) set.add("multi-tenant");
        if (properties.features().reports()) set.add("reports");
        if (properties.features().auditLog()) set.add("audit-log");
        if (properties.features().ha()) set.add("ha");
        return set;
    }

    public record LicenseStatus(boolean valid, String subject, java.time.Instant expires,
                                Set<String> licenseFeatures, Set<String> configFeatures) {
        public static LicenseStatus noLicense(boolean enterpriseEnabled) {
            return new LicenseStatus(false, null, null, Set.of(),
                    enterpriseEnabled ? Set.of() : Set.of());
        }
    }
}
