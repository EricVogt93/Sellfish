package de.sellfish.jobs.adapter.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import de.sellfish.jobs.port.JobQuery;
import de.sellfish.jobs.port.RawJob;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AdzunaSourceTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private AdzunaSource source;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        source = new AdzunaSource(builder);
    }

    @Test
    void skipsWhenCredentialsMissing() {
        assertThat(source.fetch(new JobQuery(List.of("java"), "Berlin", null, false, 10), Map.of()))
                .isEmpty();
    }

    @Test
    void parsesResults() {
        server.expect(requestTo(Matchers.containsString("/v1/api/jobs/de/search/1")))
                .andExpect(method(GET))
                .andExpect(queryParam("app_id", "id1"))
                .andExpect(queryParam("app_key", "key1"))
                .andExpect(queryParam("where", "Berlin"))
                .andRespond(withSuccess(
                        """
                        {"results":[
                          {"id":"42","title":"Java Entwickler",
                           "company":{"display_name":"Acme"},
                           "location":{"display_name":"Berlin"},
                           "description":"Spring Boot","redirect_url":"https://x/42",
                           "salary_min":60000,"salary_max":80000,"created":"2026-06-01T10:00:00Z"}
                        ]}
                        """,
                        MediaType.APPLICATION_JSON));

        List<RawJob> jobs = source.fetch(
                new JobQuery(List.of("java", "entwickler"), "Berlin", null, false, 10),
                Map.of("app_id", "id1", "app_key", "key1", "country", "de"));

        assertThat(jobs).hasSize(1);
        RawJob job = jobs.get(0);
        assertThat(job.title()).isEqualTo("Java Entwickler");
        assertThat(job.company()).isEqualTo("Acme");
        assertThat(job.externalRef()).isEqualTo("42");
        assertThat(job.salaryRaw()).contains("60000");
        assertThat(job.postedAt()).isNotNull();
        server.verify();
    }
}
