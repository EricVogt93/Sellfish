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

class OllamaClientTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private OllamaClient client;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OllamaClient(builder);
    }

    private ResolvedModel model(String baseUrl) {
        return new ResolvedModel(Provider.OLLAMA, "llama3.1", baseUrl, null);
    }

    @Test
    void supportsOnlyOllama() {
        assertThat(client.supports(Provider.OLLAMA)).isTrue();
        assertThat(client.supports(Provider.OPENAI)).isFalse();
    }

    @Test
    void chatReturnsContent() {
        server.expect(requestTo("http://localhost:11434/api/chat"))
                .andExpect(jsonPath("$.model").value("llama3.1"))
                .andRespond(withSuccess(
                        """
                        {"model":"llama3.1","message":{"role":"assistant","content":"hello"},"prompt_eval_count":10,"eval_count":3}
                        """,
                        MediaType.APPLICATION_JSON));

        ChatResult result = client.chat(model(null), ChatRequest.of("sys", "user"));
        server.verify();
        assertThat(result.content()).isEqualTo("hello");
        assertThat(result.promptTokens()).isEqualTo(10);
        assertThat(result.completionTokens()).isEqualTo(3);
    }

    @Test
    void chatThrowsOnMissingContent() {
        server.expect(requestTo("http://localhost:11434/api/chat"))
                .andRespond(withSuccess(
                        """
                        {"model":"llama3.1"}
                        """,
                        MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.chat(model(null), ChatRequest.of("s", "u")))
                .isInstanceOf(LlmException.class);
    }

    @Test
    void embedReturnsVector() {
        server.expect(requestTo("http://ollama:11434/api/embeddings"))
                .andRespond(withSuccess(
                        """
                        {"embedding":[0.1,0.2,0.3]}
                        """,
                        MediaType.APPLICATION_JSON));
        float[] vec = client.embed(model("http://ollama:11434/"), "text");
        server.verify();
        assertThat(vec).containsExactly(0.1f, 0.2f, 0.3f);
    }

    @Test
    void embedThrowsWhenNoVector() {
        server.expect(requestTo("http://localhost:11434/api/embeddings"))
                .andRespond(withSuccess(
                        """
                        {}
                        """, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.embed(model(null), "text")).isInstanceOf(LlmException.class);
    }
}
