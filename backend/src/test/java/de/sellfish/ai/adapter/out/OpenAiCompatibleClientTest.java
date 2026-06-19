package de.sellfish.ai.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
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

class OpenAiCompatibleClientTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private OpenAiCompatibleClient client;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OpenAiCompatibleClient(builder);
    }

    @Test
    void supportsOpenAiFamily() {
        assertThat(client.supports(Provider.OPENAI)).isTrue();
        assertThat(client.supports(Provider.NIM)).isTrue();
        assertThat(client.supports(Provider.OPENAI_COMPATIBLE)).isTrue();
        assertThat(client.supports(Provider.OLLAMA)).isFalse();
    }

    @Test
    void chatSendsBearerAndParsesContent() {
        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer sk-test"))
                .andExpect(jsonPath("$.model").value("gpt-4o"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andRespond(withSuccess(
                        """
                        {"model":"gpt-4o","choices":[{"message":{"content":"Hallo Welt"}}],
                         "usage":{"prompt_tokens":12,"completion_tokens":3}}
                        """,
                        MediaType.APPLICATION_JSON));

        ResolvedModel model = new ResolvedModel(Provider.OPENAI, "gpt-4o", null, "sk-test");
        ChatResult result = client.chat(model, ChatRequest.of("Sei knapp.", "Sag Hallo"));

        assertThat(result.content()).isEqualTo("Hallo Welt");
        assertThat(result.promptTokens()).isEqualTo(12);
        assertThat(result.completionTokens()).isEqualTo(3);
        server.verify();
    }

    @Test
    void embedParsesVector() {
        server.expect(requestTo("https://integrate.api.nvidia.com/v1/embeddings"))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        """
                        {"data":[{"embedding":[0.1,0.2,0.3]}]}
                        """,
                        MediaType.APPLICATION_JSON));

        ResolvedModel model = new ResolvedModel(Provider.NIM, "nv-embed", null, "key");
        float[] vector = client.embed(model, "text");

        assertThat(vector).containsExactly(0.1f, 0.2f, 0.3f);
        server.verify();
    }

    @Test
    void usesCustomBaseUrlForCompatibleProvider() {
        server.expect(requestTo("https://api.example.com/v1/chat/completions"))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        """
                        {"choices":[{"message":{"content":"ok"}}]}
                        """,
                        MediaType.APPLICATION_JSON));

        ResolvedModel model =
                new ResolvedModel(Provider.OPENAI_COMPATIBLE, "compatible-model", "https://api.example.com/v1", "key");
        assertThat(client.chat(model, ChatRequest.of("s", "u")).content()).isEqualTo("ok");
        server.verify();
    }

    @Test
    void serverErrorBecomesLlmException() {
        server.expect(requestTo("https://api.openai.com/v1/chat/completions")).andRespond(withServerError());
        ResolvedModel model = new ResolvedModel(Provider.OPENAI, "gpt-4o", null, "sk");
        assertThatThrownBy(() -> client.chat(model, ChatRequest.of("s", "u"))).isInstanceOf(LlmException.class);
    }

    @Test
    void compatibleProviderWithoutBaseUrlFails() {
        ResolvedModel model = new ResolvedModel(Provider.OPENAI_COMPATIBLE, "m", null, "k");
        assertThatThrownBy(() -> client.chat(model, ChatRequest.of("s", "u"))).isInstanceOf(LlmException.class);
    }
}
