package de.sellfish.tenant;

import de.sellfish.auth.AppUserDetails;
import de.sellfish.users.User;
import de.sellfish.users.UserRepository;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Liest den {@code X-Org-Id}-Header und setzt den aktiven Org-Kontext
 * für den aktuellen Request als Request-Attribut.
 */
@Component
public class OrgFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(OrgFilter.class);
    public static final String ATTR_ORG_ID = "ba.orgId";

    private final UserRepository userRepository;
    private final OrgMemberRepository memberRepository;

    public OrgFilter(UserRepository userRepository, OrgMemberRepository memberRepository) {
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        UUID userId = getUserIdFromSecurity();
        UUID orgId = null;
        if (userId != null) {
            String header = httpReq.getHeader("X-Org-Id");
            if (header != null && !header.isBlank()) {
                try {
                    UUID requestedOrg = UUID.fromString(header);
                    if (memberRepository.findByOrgIdAndUserId(requestedOrg, userId).isPresent()) {
                        orgId = requestedOrg;
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (orgId == null) {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null && user.getCurrentOrgId() != null) {
                    orgId = user.getCurrentOrgId();
                }
            }
        }
        if (orgId != null) {
            httpReq.setAttribute(ATTR_ORG_ID, orgId);
        }
        chain.doFilter(request, response);
    }

    public static UUID getOrgFromRequest() {
        var req = org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();
        if (req instanceof org.springframework.web.context.request.ServletRequestAttributes sra) {
            return (UUID) sra.getRequest().getAttribute(ATTR_ORG_ID);
        }
        return null;
    }

    private UUID getUserIdFromSecurity() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserDetails details) {
            return details.getId();
        }
        return null;
    }
}
