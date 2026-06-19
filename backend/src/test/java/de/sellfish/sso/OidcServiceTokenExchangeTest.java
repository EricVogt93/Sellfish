package de.sellfish.sso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import de.sellfish.common.error.ApiException;
import de.sellfish.sso.OidcProperties.ProviderConfig;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OidcServiceTokenExchangeTest {

    private KeyPair keyPair;
    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        keyPair = gen.generateKeyPair();
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
    }

    private ProviderConfig provider(String issuer) {
        return new ProviderConfig(
                "authentik",
                "Authentik",
                issuer,
                "client-123",
                "secret",
                "https://auth/o/authorize/",
                "https://auth/o/token/",
                null,
                "https://auth/o/jwks/",
                List.of("openid", "email"),
                null);
    }

    private String signedIdToken(String issuer) {
        return Jwts.builder()
                .header()
                .keyId("key-1")
                .and()
                .issuer(issuer)
                .subject("oidc-sub-42")
                .claim("email", "Eric@X.com")
                .claim("name", "Eric Vogt")
                .claim("email_verified", true)
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    private OidcService service(String issuer) {
        return new OidcService(new OidcProperties(true, "https://app/cb", List.of(provider(issuer))), builder);
    }

    private void mockJwks() {
        RSAPublicKey pub = (RSAPublicKey) keyPair.getPublic();
        String n = Base64.getUrlEncoder().encodeToString(pub.getModulus().toByteArray());
        String e = Base64.getUrlEncoder().encodeToString(pub.getPublicExponent().toByteArray());
        server.expect(requestTo(containsString("/jwks/")))
                .andRespond(withSuccess(
                        """
                        {"keys":[{"kid":"key-1","kty":"RSA","use":"sig","alg":"RS256","n":"%s","e":"%s"}]}
                        """
                                .formatted(n, e),
                        MediaType.APPLICATION_JSON));
    }

    @Test
    void exchangeCodeVerifiesTokenAndReturnsUser() {
        String issuer = "https://auth.example.com";
        String idToken = signedIdToken(issuer);
        server.expect(requestTo(containsString("/token/")))
                .andRespond(withSuccess(
                        """
                        {"access_token":"at","id_token":"%s","token_type":"Bearer"}
                        """
                                .formatted(idToken),
                        MediaType.APPLICATION_JSON));
        mockJwks();

        OidcService.OidcUser user = service(issuer).exchangeCode("authentik", "the-code");
        assertThat(user.subject()).isEqualTo("oidc-sub-42");
        assertThat(user.email()).isEqualTo("eric@x.com"); // lowercased
        assertThat(user.name()).isEqualTo("Eric Vogt");
        assertThat(user.emailVerified()).isTrue();
        server.verify();
    }

    @Test
    void exchangeCodeFailsWhenIssuerMismatch() {
        String idToken = signedIdToken("https://wrong-issuer");
        server.expect(requestTo(containsString("/token/")))
                .andRespond(withSuccess(
                        """
                        {"id_token":"%s"}
                        """.formatted(idToken),
                        MediaType.APPLICATION_JSON));
        mockJwks();

        // service configured with a different issuer -> verification fails
        assertThatThrownBy(() -> service("https://auth.example.com").exchangeCode("authentik", "code"))
                .isInstanceOf(ApiException.class);
    }
}
