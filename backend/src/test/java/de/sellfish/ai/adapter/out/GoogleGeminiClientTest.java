package de.sellfish.ai.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
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

class GoogleGeminiClientTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private GoogleGeminiClient client;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GoogleGeminiClient(builder);
    }

    private ResolvedModel model() {
        return new ResolvedModel(Provider.GOOGLE, "gemini-1.5-flash", null, "gkey");
    }

    @Test
    void supportsOnlyGoogle() {
        assertThat(client.supports(Provider.GOOGLE)).isTrue();
        assertThat(client.supports(Provider.OPENAI)).isFalse();
    }

    @Test
    void chatReturnsContent() {
        server.expect(
                        requestTo(
                                "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=gkey"))
                .andRespond(withSuccess(
                        """
                        {"candidates":[{"content":{"parts":[{"text":"hello"}]}}],"usageMetadata":{"promptTokenCount":3,"candidatesTokenCount":1}}
                        """,
                        MediaType.APPLICATION_JSON));

        ChatResult result = client.chat(model(), ChatRequest.of("sys", "user"));
        server.verify();
        assertThat(result.content()).isEqualTo("hello");
        assertThat(result.promptTokens()).isEqualTo(3);
    }

    @Test
    void chatThrowsOnMissingContent() {
        server.expect(
                        requestTo(
                                "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=gkey"))
                .andRespond(withSuccess(
                        """
                        {}
                        """, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.chat(model(), ChatRequest.of("s", "u"))).isInstanceOf(LlmException.class);
    }

    @Test
    void embedReturnsVector() {
        server.expect(
                        requestTo(
                                "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:embedContent?key=gkey"))
                .andRespond(withSuccess(
                        """
                        {"embedding":{"values":[0.4,0.5]}}
                        """,
                        MediaType.APPLICATION_JSON));
        assertThat(client.embed(model(), "text")).containsExactly(0.4f, 0.5f);
        server.verify();
    }

    @Test
    void embedThrowsWhenNoVector() {
        server.expect(
                        requestTo(
                                "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:embedContent?key=gkey"))
                .andRespond(withSuccess(
                        """
                        {}
                        """, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.embed(model(), "text")).isInstanceOf(LlmException.class);
    }
}
