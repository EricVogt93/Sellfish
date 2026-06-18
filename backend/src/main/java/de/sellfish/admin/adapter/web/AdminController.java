package de.sellfish.admin.adapter.web;
import de.sellfish.admin.*;

import de.sellfish.jobs.adapter.source.SourceCountries;
import de.sellfish.ai.LlmProviderConfig;
import de.sellfish.ai.Provider;
import de.sellfish.ai.Purpose;
import de.sellfish.jobs.JobSourceConfig;
import de.sellfish.users.Role;
import de.sellfish.users.User;
import de.sellfish.users.UserStatus;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminController {

    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    // --- Nutzer ---

    public record UserView(UUID id, String email, Role role, UserStatus status, Instant createdAt) {
        static UserView from(User u) {
            return new UserView(u.getId(), u.getEmail(), u.getRole(), u.getStatus(), u.getCreatedAt());
        }
    }

    public record RoleRequest(Role role) {
    }

    public record StatusRequest(UserStatus status) {
    }

    @GetMapping("/users")
    public List<UserView> users() {
        return service.listUsers().stream().map(UserView::from).toList();
    }

    @PostMapping("/users/{id}/role")
    public UserView setRole(@PathVariable UUID id, @RequestBody RoleRequest req) {
        return UserView.from(service.setRole(id, req.role()));
    }

    @PostMapping("/users/{id}/status")
    public UserView setStatus(@PathVariable UUID id, @RequestBody StatusRequest req) {
        return UserView.from(service.setStatus(id, req.status()));
    }

    // --- Job-Quellen ---

    public record JobSourceView(String code, String name, boolean enabled, String config) {
        static JobSourceView from(JobSourceConfig s) {
            return new JobSourceView(s.getCode(), s.getName(), s.isEnabled(), s.getConfig());
        }
    }

    public record JobSourceRequest(Boolean enabled, String config) {
    }

    @GetMapping("/job-sources")
    public List<JobSourceView> jobSources() {
        return service.listJobSources().stream().map(JobSourceView::from).toList();
    }

    @PutMapping("/job-sources/{code}")
    public JobSourceView updateJobSource(@PathVariable String code, @RequestBody JobSourceRequest req) {
        return JobSourceView.from(service.updateJobSource(code, req.enabled(), req.config()));
    }

    // --- Globale LLM-Konfiguration ---

    public record GlobalLlmView(UUID id, Provider provider, String model, Purpose purpose,
                                String baseUrl, boolean isDefault, boolean hasKey) {
        static GlobalLlmView from(LlmProviderConfig c) {
            boolean hasKey = (c.getKeyEnc() != null && !c.getKeyEnc().isBlank())
                    || (c.getKeyRef() != null && !c.getKeyRef().isBlank());
            return new GlobalLlmView(c.getId(), c.getProvider(), c.getModel(), c.getPurpose(),
                    c.getBaseUrl(), c.isDefault(), hasKey);
        }
    }

    public record GlobalLlmRequest(Provider provider, String model, Purpose purpose,
                                   String baseUrl, String keyRef, String apiKey, Boolean isDefault) {
    }

    @GetMapping("/llm-configs")
    public List<GlobalLlmView> globalLlmConfigs() {
        return service.listGlobalLlmConfigs().stream().map(GlobalLlmView::from).toList();
    }

    @PostMapping("/llm-configs")
    @ResponseStatus(HttpStatus.CREATED)
    public GlobalLlmView createGlobalLlmConfig(@RequestBody GlobalLlmRequest req) {
        return GlobalLlmView.from(service.createGlobalLlmConfig(
                req.provider(), req.model(), req.purpose(), req.baseUrl(), req.keyRef(),
                req.apiKey(), Boolean.TRUE.equals(req.isDefault())));
    }

    @DeleteMapping("/llm-configs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGlobalLlmConfig(@PathVariable UUID id) {
        service.deleteGlobalLlmConfig(id);
    }

    // --- Länder-Info für Job-Quellen ---

    @GetMapping("/source-countries")
    public List<SourceCountries.CountryGroup> sourceCountries() {
        return SourceCountries.allGroups();
    }
}
