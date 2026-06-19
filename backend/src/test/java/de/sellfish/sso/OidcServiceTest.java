package de.sellfish.sso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.sellfish.common.error.ApiException;
import de.sellfish.sso.OidcProperties.ProviderConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class OidcServiceTest {

    private final RestClient.Builder builder = RestClient.builder();

    private ProviderConfig provider() {
        return new ProviderConfig(
                "authentik",
                "Authentik",
                "https://auth.example.com",
                "client-123",
                "secret",
                "https://auth.example.com/o/authorize/",
                "https://auth.example.com/o/token/",
                null,
                "https://auth.example.com/o/jwks/",
                List.of("openid", "email"),
                null);
    }

    private OidcService service(boolean enabled, List<ProviderConfig> providers) {
        return new OidcService(new OidcProperties(enabled, "https://app/cb", providers), builder);
    }

    @Test
    void buildAuthUrlContainsRequiredParams() {
        OidcService service = service(true, List.of(provider()));
        String url = service.buildAuthUrl("authentik");
        assertThat(url)
                .contains("client_id=client-123")
                .contains("response_type=code")
                .contains("redirect_uri=")
                .contains("openid")
                .contains("email");
    }

    @Test
    void buildAuthUrlThrowsForUnknownProvider() {
        OidcService service = service(true, List.of(provider()));
        assertThatThrownBy(() -> service.buildAuthUrl("ghost")).isInstanceOf(ApiException.class);
    }

    @Test
    void hasProvidersReflectsConfig() {
        assertThat(service(true, List.of(provider())).hasProviders()).isTrue();
        assertThat(service(false, List.of(provider())).hasProviders()).isFalse();
        assertThat(service(true, List.of()).hasProviders()).isFalse();
    }

    @Test
    void providerListMapsIdAndName() {
        var list = service(true, List.of(provider())).providerList();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).id()).isEqualTo("authentik");
        assertThat(list.get(0).name()).isEqualTo("Authentik");
    }

    @Test
    void providerListEmptyWhenDisabled() {
        assertThat(service(false, List.of(provider())).providerList()).isEmpty();
    }
}
