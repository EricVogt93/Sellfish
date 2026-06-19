package de.sellfish.tenant;

import de.sellfish.auth.AuthService;
import de.sellfish.auth.dto.AuthDtos.TokenResponse;
import de.sellfish.common.security.SecurityUtil;
import de.sellfish.users.User;
import de.sellfish.users.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orgs")
public class OrgController {

    private final OrgService service;
    private final AuthService authService;
    private final UserRepository userRepository;

    public OrgController(OrgService service, AuthService authService, UserRepository userRepository) {
        this.service = service;
        this.authService = authService;
        this.userRepository = userRepository;
    }

    public record OrgView(UUID id, String name, String slug, String plan) {
        static OrgView from(Organization o) {
            return new OrgView(o.getId(), o.getName(), o.getSlug(), o.getPlan());
        }
    }

    public record CreateOrgRequest(String name, String slug) {}

    public record MemberView(UUID userId, String email, OrgMemberRole role, java.time.Instant joinedAt) {
        static MemberView from(OrganizationMember m) {
            return new MemberView(m.getUserId(), null, m.getRole(), m.getJoinedAt());
        }

        static MemberView fromUser(de.sellfish.users.User user, OrganizationMember member) {
            return new MemberView(user.getId(), user.getEmail(), member.getRole(), member.getJoinedAt());
        }
    }

    public record SwitchOrgRequest(UUID orgId) {}

    public record AddMemberRequest(UUID userId, OrgMemberRole role) {}

    @GetMapping
    public List<OrgView> mine() {
        UUID userId = SecurityUtil.currentUserId();
        return service.myOrganizations(userId).stream().map(OrgView::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrgView create(@RequestBody CreateOrgRequest req) {
        return OrgView.from(service.create(SecurityUtil.currentUserId(), req.name(), req.slug()));
    }

    @GetMapping("/{orgId}/members")
    public List<MemberView> members(@PathVariable UUID orgId) {
        UUID userId = SecurityUtil.currentUserId();
        List<OrganizationMember> members = service.members(orgId, userId);
        List<de.sellfish.users.User> users = service.listOrgUsers(orgId, userId);
        return members.stream()
                .map(m -> {
                    var user = users.stream()
                            .filter(u -> u.getId().equals(m.getUserId()))
                            .findFirst();
                    return user.map(u -> MemberView.fromUser(u, m)).orElseGet(() -> MemberView.from(m));
                })
                .toList();
    }

    @PostMapping("/switch")
    public TokenResponse switchOrg(@RequestBody SwitchOrgRequest req) {
        UUID userId = SecurityUtil.currentUserId();
        service.switchContext(userId, req.orgId());
        User user = userRepository.findById(userId).orElseThrow();
        return authService.issueAccessToken(user);
    }

    @PostMapping("/{orgId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberView addMember(@PathVariable UUID orgId, @RequestBody AddMemberRequest req) {
        UUID userId = SecurityUtil.currentUserId();
        return MemberView.from(service.addMember(orgId, userId, req.userId(), req.role()));
    }

    @DeleteMapping("/{orgId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable UUID orgId, @PathVariable UUID targetUserId) {
        service.removeMember(orgId, SecurityUtil.currentUserId(), targetUserId);
    }
}
