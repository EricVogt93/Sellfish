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

/**
 * Tests der keyless internationalen Quellen (Parsing + clientseitige Filterung).
 */
class KeylessJobSourcesTest {

    private final JobQuery query = new JobQuery(List.of("java"), "Berlin", null, false, 10);

    private record Bound(RestClient.Builder builder, MockRestServiceServer server) {}

    private Bound bind() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        return new Bound(builder, server);
    }

    @Test
    void arbeitnowParsesAndFilters() {
        Bound b = bind();
        b.server()
                .expect(requestTo(Matchers.containsString("/job-board-api")))
                .andRespond(withSuccess(
                        """
                        {"data":[
                          {"slug":"a1","title":"Java Engineer","company_name":"Acme",
                           "location":"Berlin","remote":true,"url":"https://x/a1","description":"Spring",
                           "created_at":1700000000},
                          {"slug":"b2","title":"Sales Manager","company_name":"Beta",
                           "location":"Berlin","url":"https://x/b2","description":"Vertrieb"}
                        ]}
                        """,
                        MediaType.APPLICATION_JSON));
        ArbeitnowSource source = new ArbeitnowSource(b.builder());

        List<RawJob> jobs = source.fetch(query, Map.of());
        assertThat(jobs).hasSize(1); // "Sales Manager" passt nicht zu "java"
        assertThat(jobs.get(0).title()).isEqualTo("Java Engineer");
        assertThat(jobs.get(0).remote()).isEqualTo("REMOTE");
        assertThat(jobs.get(0).postedAt()).isNotNull();
    }

    @Test
    void remotiveParsesJobs() {
        Bound b = bind();
        b.server()
                .expect(requestTo(Matchers.containsString("/remote-jobs")))
                .andRespond(withSuccess(
                        """
                        {"jobs":[
                          {"id":7,"title":"Java Dev","company_name":"Acme",
                           "candidate_required_location":"Worldwide","url":"https://r/7",
                           "description":"x","publication_date":"2026-06-01T10:00:00","salary":"€70k"}
                        ]}
                        """,
                        MediaType.APPLICATION_JSON));
        RemotiveSource source = new RemotiveSource(b.builder());

        List<RawJob> jobs = source.fetch(query, Map.of());
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).company()).isEqualTo("Acme");
        assertThat(jobs.get(0).remote()).isEqualTo("REMOTE");
        assertThat(jobs.get(0).salaryRaw()).isEqualTo("€70k");
    }

    @Test
    void remoteOkSkipsLegalAndFilters() {
        Bound b = bind();
        b.server()
                .expect(requestTo(Matchers.containsString("/api")))
                .andRespond(withSuccess(
                        """
                        [
                          {"legal":"By using this API you agree..."},
                          {"id":"1","position":"Java Engineer","company":"Acme","location":"Remote",
                           "url":"https://remoteok.com/1","description":"Spring","date":"2026-06-01T00:00:00+00:00"},
                          {"id":"2","position":"Designer","company":"Beta","url":"https://remoteok.com/2",
                           "description":"Figma","date":"2026-06-01T00:00:00+00:00"}
                        ]
                        """,
                        MediaType.APPLICATION_JSON));
        RemoteOkSource source = new RemoteOkSource(b.builder());

        List<RawJob> jobs = source.fetch(query, Map.of());
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).title()).isEqualTo("Java Engineer");
        assertThat(jobs.get(0).sourceCode()).isEqualTo("REMOTEOK");
    }

    @Test
    void jobicyParsesJobs() {
        Bound b = bind();
        b.server()
                .expect(requestTo(Matchers.containsString("/remote-jobs")))
                .andRespond(withSuccess(
                        """
                        {"jobs":[
                          {"id":42,"jobTitle":"Java Developer","companyName":"Acme","jobGeo":"Anywhere",
                           "url":"https://j/42","jobExcerpt":"Spring","pubDate":"2026-06-01 10:00:00"}
                        ]}
                        """,
                        MediaType.APPLICATION_JSON));
        JobicySource source = new JobicySource(b.builder());

        List<RawJob> jobs = source.fetch(query, Map.of());
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).title()).isEqualTo("Java Developer");
    }

    @Test
    void himalayasParsesAndFilters() {
        Bound b = bind();
        b.server()
                .expect(requestTo(Matchers.containsString("/jobs/api")))
                .andRespond(withSuccess(
                        """
                        {"jobs":[
                          {"guid":"g1","title":"Java Backend","companyName":"Acme",
                           "locationRestrictions":["EU"],"applicationLink":"https://h/g1",
                           "description":"Spring","pubDate":1700000000},
                          {"guid":"g2","title":"Recruiter","companyName":"Beta",
                           "locationRestrictions":[],"description":"HR","pubDate":1700000000}
                        ]}
                        """,
                        MediaType.APPLICATION_JSON));
        HimalayasSource source = new HimalayasSource(b.builder());

        List<RawJob> jobs = source.fetch(query, Map.of());
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).title()).isEqualTo("Java Backend");
        assertThat(jobs.get(0).location()).isEqualTo("EU");
    }

    @Test
    void theMuseParsesJobs() {
        Bound b = bind();
        b.server()
                .expect(requestTo(Matchers.containsString("/jobs")))
                .andRespond(withSuccess(
                        """
                        {"results":[
                          {"id":5,"name":"Java Engineer","company":{"name":"Acme"},
                           "locations":[{"name":"Berlin, Germany"}],
                           "refs":{"landing_page":"https://m/5"},"contents":"Spring",
                           "publication_date":"2026-06-01T10:00:00Z"}
                        ]}
                        """,
                        MediaType.APPLICATION_JSON));
        TheMuseSource source = new TheMuseSource(b.builder());

        List<RawJob> jobs = source.fetch(query, Map.of());
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).company()).isEqualTo("Acme");
        assertThat(jobs.get(0).location()).isEqualTo("Berlin, Germany");
        assertThat(jobs.get(0).url()).isEqualTo("https://m/5");
    }
}
