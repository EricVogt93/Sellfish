package de.sellfish.sso;

import de.sellfish.auth.AuthService;
import de.sellfish.auth.dto.AuthDtos.TokenResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/sso")
public class SsoController {

    private final OidcService oidcService;
    private final AuthService authService;

    public SsoController(OidcService oidcService, AuthService authService) {
        this.oidcService = oidcService;
        this.authService = authService;
    }

    public record ProviderView(String id, String name) {
        static ProviderView from(OidcService.ProviderInfo p) {
            return new ProviderView(p.id(), p.name());
        }
    }

    public record AuthUrlResponse(String authUrl) {}

    public record CallbackRequest(String code, String provider) {}

    @GetMapping("/providers")
    public List<ProviderView> providers() {
        return oidcService.providerList().stream().map(ProviderView::from).toList();
    }

    @GetMapping("/{provider}/login")
    public AuthUrlResponse login(@PathVariable String provider) {
        return new AuthUrlResponse(oidcService.buildAuthUrl(provider));
    }

    @PostMapping("/callback")
    public TokenResponse callback(@RequestBody CallbackRequest req) {
        OidcService.OidcUser oidcUser = oidcService.exchangeCode(req.provider(), req.code());
        return authService.loginWithSso(oidcUser);
    }
}
