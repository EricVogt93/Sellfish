package de.sellfish.jobs.adapter.source;
import de.sellfish.jobs.port.JobSource;
import de.sellfish.jobs.port.JobQuery;
import de.sellfish.jobs.port.RawJob;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

class BundesagenturSourceTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private BundesagenturSource source;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        source = new BundesagenturSource(builder);
    }

    @Test
    void parsesListAndFetchesDescription() {
        server.expect(requestTo(Matchers.containsString("/pc/v4/app/jobs")))
                .andExpect(method(GET))
                .andExpect(header("X-API-Key", "jobboerse-jobsuche"))
                .andExpect(queryParam("was", "java"))
                .andRespond(withSuccess("""
                        {"stellenangebote":[
                          {"titel":"Java Entwickler","refnr":"10000-ABC",
                           "arbeitgeber":"Acme GmbH",
                           "arbeitsort":{"plz":"10115","ort":"Berlin","region":"Berlin"},
                           "aktuelleVeroeffentlichungsdatum":"2026-06-01"}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(Matchers.containsString("/pc/v4/jobdetails/")))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {"stellenbeschreibung":"Wir suchen einen Java-Entwickler."}
                        """, MediaType.APPLICATION_JSON));

        List<RawJob> jobs = source.fetch(
                new JobQuery(List.of("java"), "Berlin", 50, false, 10), Map.of());

        assertThat(jobs).hasSize(1);
        RawJob job = jobs.get(0);
        assertThat(job.title()).isEqualTo("Java Entwickler");
        assertThat(job.company()).isEqualTo("Acme GmbH");
        assertThat(job.location()).isEqualTo("10115 Berlin");
        assertThat(job.description()).contains("Java-Entwickler");
        assertThat(job.externalRef()).isEqualTo("10000-ABC");
        assertThat(job.url()).contains("10000-ABC");
        assertThat(job.postedAt()).isNotNull();
        server.verify();
    }

    @Test
    void emptyResponseYieldsEmptyList() {
        server.expect(requestTo(Matchers.containsString("/pc/v4/app/jobs")))
                .andRespond(withSuccess("{\"stellenangebote\":[]}", MediaType.APPLICATION_JSON));
        assertThat(source.fetch(new JobQuery(List.of("x"), null, null, false, 5), Map.of())).isEmpty();
    }
}
