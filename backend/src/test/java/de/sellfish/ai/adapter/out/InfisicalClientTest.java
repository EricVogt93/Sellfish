package de.sellfish.ai.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import de.sellfish.common.config.InfisicalProperties;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class InfisicalClientTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;

    private InfisicalProperties props(boolean enabled) {
        return new InfisicalProperties(enabled, "https://inf.example.com", "cid", "csec", "proj", "dev");
    }

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
    }

    @Test
    void isEnabledReflectsConfig() {
        assertThat(new InfisicalClient(props(true), builder).isEnabled()).isTrue();
        assertThat(new InfisicalClient(props(false), builder).isEnabled()).isFalse();
    }

    @Test
    void getSecretReturnsEmptyWhenDisabled() {
        InfisicalClient client = new InfisicalClient(props(false), builder);
        assertThat(client.getSecret("/llm/key")).isEmpty();
        assertThat(client.getSecret(null)).isEmpty();
        assertThat(client.getSecret("  ")).isEmpty();
    }

    @Test
    void getSecretFetchesValueAfterLogin() {
        // login response
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/universal-auth/login")))
                .andRespond(withSuccess(
                        """
                        {"accessToken":"tok-123","expiresIn":3600}
                        """,
                        MediaType.APPLICATION_JSON));
        // secret response
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/secrets/raw/")))
                .andRespond(withSuccess(
                        """
                        {"secret":{"secretValue":"super-secret"}}
                        """,
                        MediaType.APPLICATION_JSON));
        InfisicalClient client = new InfisicalClient(props(true), builder);
        Optional<String> value = client.getSecret("llm/openai-key");
        server.verify();
        assertThat(value).contains("super-secret");
    }
}
