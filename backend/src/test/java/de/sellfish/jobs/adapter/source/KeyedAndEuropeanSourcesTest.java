package de.sellfish.jobs.adapter.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import de.sellfish.jobs.port.JobQuery;
import de.sellfish.jobs.port.RawJob;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KeyedAndEuropeanSourcesTest {

    private final JobQuery query = new JobQuery(List.of("java"), "Berlin", null, false, 10);

    private record Bound(RestClient.Builder builder, MockRestServiceServer server) {}

    private Bound bind() {
        RestClient.Builder builder = RestClient.builder();
        return new Bound(
                builder,
                MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build());
    }

    @Test
    void careerJetSkipsWithoutApiKey() {
        // no api_key in config -> returns empty without any HTTP call
        Bound b = bind();
        List<RawJob> jobs = new CareerJetSource(b.builder()).fetch(query, Map.of());
        assertThat(jobs).isEmpty();
    }

    @Test
    void careerJetParsesJobsArray() {
        Bound b = bind();
        b.server()
                .expect(requestTo(Matchers.containsString("careerjet.com/api/search")))
                .andRespond(withSuccess(
                        """
                        {"jobs":[
                          {"id":"cj1","title":"Java Dev","company":"Acme","location":"Berlin",
                           "url":"https://careerjet.com/cj1","description":"Spring","salary":"70k"}
                        ]}
                        """,
                        MediaType.APPLICATION_JSON));
        List<RawJob> jobs = new CareerJetSource(b.builder()).fetch(query, Map.of("api_key", "affkey"));
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).title()).isEqualTo("Java Dev");
        assertThat(jobs.get(0).salaryRaw()).isEqualTo("70k");
    }

    @Test
    void europeRemotelyParsesAndFilters() {
        Bound b = bind();
        b.server()
                .expect(requestTo(Matchers.containsString("/jobs.json")))
                .andRespond(withSuccess(
                        """
                        [
                          {"id":"e1","position":"Java Engineer","company":"Acme","location":"EU",
                           "description":"Spring","url":"https://eur/e1","salary":"€70k","date":"2026-06-01T00:00:00Z"},
                          {"id":"e2","position":"Designer","company":"Beta","description":"Figma","url":"https://eur/e2"}
                        ]
                        """,
                        MediaType.APPLICATION_JSON));
        List<RawJob> jobs = new EuropeRemotelySource(b.builder()).fetch(query, Map.of());
        assertThat(jobs).hasSize(1); // Designer filtered out (no "java")
        assertThat(jobs.get(0).salaryRaw()).isEqualTo("€70k");
    }

    @Test
    void europeRemotelyReturnsEmptyWhenNotArray() {
        Bound b = bind();
        b.server()
                .expect(requestTo(Matchers.containsString("/jobs.json")))
                .andRespond(withSuccess(
                        """
                        {"error":1}
                        """,
                        MediaType.APPLICATION_JSON));
        assertThat(new EuropeRemotelySource(b.builder()).fetch(query, Map.of())).isEmpty();
    }
}
