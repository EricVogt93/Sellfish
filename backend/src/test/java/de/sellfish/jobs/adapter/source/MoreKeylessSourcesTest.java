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

class MoreKeylessSourcesTest {

    private final JobQuery query = new JobQuery(List.of("java"), "Berlin", null, false, 10);

    private record Bound(RestClient.Builder builder, MockRestServiceServer server) {}

    private Bound bind() {
        RestClient.Builder builder = RestClient.builder();
        return new Bound(
                builder,
                MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build());
    }

    @Test
    void fourScottyParsesDataArray() {
        Bound b = bind();
        b.server()
                .expect(requestTo(Matchers.containsString("/v2/jobs")))
                .andRespond(withSuccess(
                        """
                        {"data":[
                          {"id":"a1","title":"Java Dev","company":{"name":"Acme"},
                           "location":"Berlin","remote":true,"url":"https://4/a1","publishedAt":"2026-06-01T00:00:00Z",
                           "skills":["spring","kafka"],"tasks":["build"],"salary":{"min":"70k","max":"90k","currency":"EUR"}}
                        ]}
                        """,
                        MediaType.APPLICATION_JSON));
        List<RawJob> jobs = new FourScottySource(b.builder()).fetch(query, Map.of());
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).company()).isEqualTo("Acme");
        assertThat(jobs.get(0).remote()).isEqualTo("REMOTE");
        assertThat(jobs.get(0).salaryRaw()).contains("70k");
        assertThat(jobs.get(0).description()).contains("spring");
    }

    @Test
    void fourScottyReturnsEmptyOnMissingData() {
        Bound b = bind();
        b.server()
                .expect(requestTo(Matchers.containsString("/v2/jobs")))
                .andRespond(withSuccess(
                        """
                        {"data":[]}
                        """,
                        MediaType.APPLICATION_JSON));
        assertThat(new FourScottySource(b.builder()).fetch(query, Map.of())).isEmpty();
    }

    @Test
    void honeypotParsesResultsArray() {
        Bound b = bind();
        b.server()
                .expect(requestTo(Matchers.containsString("/jobs/search")))
                .andRespond(withSuccess(
                        """
                        {"results":[
                          {"id":1,"title":"Java Backend","company":{"name":"Honey"},
                           "locations":[{"city":"Berlin"}],"remote":true,
                           "description":"Spring","url":"/jobs/1","salary":{"display":"€70-90k"},
                           "published_at":"2026-06-01T10:00:00Z"}
                        ]}
                        """,
                        MediaType.APPLICATION_JSON));
        List<RawJob> jobs = new HoneypotSource(b.builder()).fetch(query, Map.of());
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).location()).isEqualTo("Berlin");
        assertThat(jobs.get(0).url()).contains("/jobs/1");
        assertThat(jobs.get(0).salaryRaw()).isEqualTo("€70-90k");
    }

    @Test
    void itTalentsParsesContentArray() {
        Bound b = bind();
        b.server()
                .expect(requestTo(Matchers.containsString("/jobs")))
                .andRespond(withSuccess(
                        """
                        {"content":[
                          {"id":"42","title":"Java Entwickler","company":{"name":"ITG"},
                           "location":{"city":"München"},"remote":false,
                           "description":"Spring","slug":"java-entwickler","publishedAt":"2026-06-01T00:00:00Z"}
                        ]}
                        """,
                        MediaType.APPLICATION_JSON));
        List<RawJob> jobs = new ItTalentsSource(b.builder()).fetch(query, Map.of());
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).title()).isEqualTo("Java Entwickler");
        assertThat(jobs.get(0).location()).isEqualTo("München");
        assertThat(jobs.get(0).url()).contains("java-entwickler-42");
    }

    @Test
    void itTalentsReturnsEmptyWithoutContent() {
        Bound b = bind();
        b.server()
                .expect(requestTo(Matchers.containsString("/jobs")))
                .andRespond(withSuccess(
                        """
                        {}
                        """, MediaType.APPLICATION_JSON));
        assertThat(new ItTalentsSource(b.builder()).fetch(query, Map.of())).isEmpty();
    }
}
