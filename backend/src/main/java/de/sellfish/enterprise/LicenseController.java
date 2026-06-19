package de.sellfish.enterprise;

import de.sellfish.audit.AuditAction;
import de.sellfish.audit.AuditService;
import de.sellfish.common.security.SecurityUtil;
import de.sellfish.enterprise.LicenseService.LicenseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/license")
public class LicenseController {

    private final LicenseService licenseService;
    private final AuditService auditService;

    public LicenseController(LicenseService licenseService, AuditService auditService) {
        this.licenseService = licenseService;
        this.auditService = auditService;
    }

    // ... rest same
    public record LicenseRequest(String licenseKey) {}

    public record LicenseResponse(
            boolean valid, String subject, java.time.Instant expires, java.util.Set<String> features) {
        static LicenseResponse from(LicenseStatus s) {
            if (!s.valid()) {
                return new LicenseResponse(false, null, null, s.licenseFeatures());
            }
            java.util.Set<String> all = new java.util.LinkedHashSet<>(s.licenseFeatures());
            all.addAll(s.configFeatures());
            return new LicenseResponse(true, s.subject(), s.expires(), all);
        }
    }

    @GetMapping("/status")
    public LicenseResponse status() {
        return LicenseResponse.from(licenseService.getStatus());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public LicenseResponse upload(@RequestBody LicenseRequest req) {
        licenseService.uploadLicense(req.licenseKey());
        auditService.record(SecurityUtil.currentUserId(), AuditAction.LICENSE_UPLOAD);
        return LicenseResponse.from(licenseService.getStatus());
    }
}
