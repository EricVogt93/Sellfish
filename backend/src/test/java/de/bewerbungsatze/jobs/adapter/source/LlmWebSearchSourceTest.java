package de.bewerbungsatze.jobs.adapter.source;
import de.bewerbungsatze.jobs.port.JobSource;
import de.bewerbungsatze.jobs.port.JobQuery;
import de.bewerbungsatze.jobs.port.RawJob;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bewerbungsatze.ai.LlmException;
import de.bewerbungsatze.ai.LlmService;
import de.bewerbungsatze.ai.model.ChatRequest;
import de.bewerbungsatze.ai.model.ChatResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmWebSearchSourceTest {

    private final LlmService llmService = mock(LlmService.class);
    private final LlmWebSearchSource source = new LlmWebSearchSource(llmService, new ObjectMapper());

    @Test
    void parsesJsonArrayFromModel() {
        when(llmService.chat(nullable(UUID.class), any(ChatRequest.class)))
                .thenReturn(new ChatResult("""
                        ```json
                        [{"title":"Java Entwickler","company":"Acme","location":"Berlin",
                          "url":"https://x/1","description":"Spring"},
                         {"title":"","company":"Leer"}]
                        ```
                        """, "m", null, null));

        List<RawJob> jobs = source.fetch(new JobQuery(List.of("java"), "Berlin", null, false, 5), Map.of());

        assertThat(jobs).hasSize(1); // leerer Titel wird verworfen
        assertThat(jobs.get(0).title()).isEqualTo("Java Entwickler");
        assertThat(jobs.get(0).url()).isEqualTo("https://x/1");
        assertThat(jobs.get(0).sourceCode()).isEqualTo("LLM_WEB");
    }

    @Test
    void returnsEmptyWhenLlmUnavailable() {
        when(llmService.chat(nullable(UUID.class), any(ChatRequest.class)))
                .thenThrow(new LlmException("kein Provider"));
        assertThat(source.fetch(new JobQuery(List.of("x"), null, null, false, 5), Map.of())).isEmpty();
    }
}
