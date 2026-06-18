package de.sellfish.audit;

import de.sellfish.common.security.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/audit")
public class AuditController {

    private final AuditRepository repository;

    public AuditController(AuditRepository repository) {
        this.repository = repository;
    }

    public record AuditView(UUID id, UUID userId, UUID orgId, String action,
                            String targetType, String targetId, String details, String ip, Instant ts) {
        static AuditView from(AuditEvent e) {
            return new AuditView(e.getId(), e.getUserId(), e.getOrgId(),
                    e.getAction().name(), e.getTargetType(), e.getTargetId(),
                    e.getDetails(), e.getIp(), e.getTs());
        }
    }

    @GetMapping
    public Page<AuditView> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) UUID orgId,
            @RequestParam(required = false) UUID userId) {

        PageRequest pr = PageRequest.of(Math.max(0, page), Math.min(200, Math.max(1, size)));

        Page<AuditEvent> result;
        if (orgId != null && userId != null) {
            result = repository.findByOrgIdAndUserIdOrderByTsDesc(orgId, userId, pr);
        } else if (orgId != null) {
            result = repository.findByOrgIdOrderByTsDesc(orgId, pr);
        } else if (userId != null) {
            result = repository.findByUserIdOrderByTsDesc(userId, pr);
        } else {
            result = repository.findAllByOrderByTsDesc(pr);
        }
        return result.map(AuditView::from);
    }
}
