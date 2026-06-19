package de.sellfish.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.sellfish.tenant.OrgFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    public AuditService(AuditRepository repository) {
        this.repository = repository;
    }

    @Async
    public void record(UUID userId, UUID orgId, AuditAction action, String targetType, String targetId, Map<String, Object> details) {
        try {
            String json = mapper.writeValueAsString(details != null ? details : Map.of());
            AuditEvent event = new AuditEvent(orgId, userId, action, targetType, targetId, json);
            event.setIp(clientIp());
            repository.save(event);
        } catch (JsonProcessingException e) {
            log.warn("Audit event not serializable: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Audit event not stored: {}", e.getMessage());
        }
    }

    public void record(UUID userId, AuditAction action, String targetType, String targetId) {
        record(userId, currentOrgId(), action, targetType, targetId, null);
    }

    public void record(UUID userId, AuditAction action) {
        record(userId, action, null, null);
    }

    private UUID currentOrgId() {
        try {
            return OrgFilter.getOrgFromRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private String clientIp() {
        try {
            var attrs = RequestContextHolder.currentRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                HttpServletRequest req = sra.getRequest();
                String forwarded = req.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    return forwarded.split(",")[0].trim();
                }
                return req.getRemoteAddr();
            }
        } catch (Exception ignored) {}
        return null;
    }
}
