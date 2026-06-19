package de.sellfish.ai.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import de.sellfish.ai.LlmException;
import de.sellfish.ai.Provider;
import de.sellfish.ai.model.ChatRequest;
import de.sellfish.ai.model.ChatResult;
import de.sellfish.ai.model.ResolvedModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AnthropicClientTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private AnthropicClient client;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AnthropicClient(builder);
    }

    private ResolvedModel model() {
        return new ResolvedModel(Provider.ANTHROPIC, "claude-sonnet-4-6", null, "sk-ant");
    }

    @Test
    void supportsOnlyAnthropic() {
        assertThat(client.supports(Provider.ANTHROPIC)).isTrue();
        assertThat(client.supports(Provider.OPENAI)).isFalse();
    }

    @Test
    void chatReturnsContent() {
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(header("x-api-key", "sk-ant"))
                .andExpect(jsonPath("$.model").value("claude-sonnet-4-6"))
                .andRespond(withSuccess(
                        """
                        {"model":"claude-sonnet-4-6","content":[{"type":"text","text":"hi"}],"usage":{"input_tokens":5,"output_tokens":2}}
                        """,
                        MediaType.APPLICATION_JSON));

        ChatResult result = client.chat(model(), ChatRequest.of("sys", "user"));
        server.verify();
        assertThat(result.content()).isEqualTo("hi");
        assertThat(result.promptTokens()).isEqualTo(5);
        assertThat(result.completionTokens()).isEqualTo(2);
    }

    @Test
    void chatThrowsOnMissingContent() {
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess(
                        """
                        {"model":"claude"}
                        """,
                        MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.chat(model(), ChatRequest.of("s", "u"))).isInstanceOf(LlmException.class);
    }
}
