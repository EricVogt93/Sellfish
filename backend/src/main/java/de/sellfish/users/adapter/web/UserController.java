package de.sellfish.users.adapter.web;

import de.sellfish.common.error.ApiException;
import de.sellfish.common.security.CurrentUser;
import de.sellfish.tenant.OrgMemberRepository;
import de.sellfish.tenant.OrganizationRepository;
import de.sellfish.users.*;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class UserController {

    private final UserRepository userRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final OrganizationRepository orgRepository;

    public UserController(
            UserRepository userRepository,
            OrgMemberRepository orgMemberRepository,
            OrganizationRepository orgRepository) {
        this.userRepository = userRepository;
        this.orgMemberRepository = orgMemberRepository;
        this.orgRepository = orgRepository;
    }

    public record MeResponse(UUID id, String email, String role, String locale, UUID currentOrgId, List<OrgRef> orgs) {
        public record OrgRef(UUID id, String name, String slug, String plan) {}
    }

    @GetMapping
    public MeResponse me() {
        User user =
                userRepository.findById(CurrentUser.id()).orElseThrow(() -> ApiException.notFound("User not found"));
        List<MeResponse.OrgRef> orgList = orgMemberRepository.findByUserId(user.getId()).stream()
                .map(m -> orgRepository.findById(m.getOrgId()).orElse(null))
                .filter(o -> o != null)
                .map(o -> new MeResponse.OrgRef(o.getId(), o.getName(), o.getSlug(), o.getPlan()))
                .toList();
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getLocale(),
                user.getCurrentOrgId(),
                orgList);
    }
}
