package de.sellfish.jobs.adapter.source;
import de.sellfish.jobs.port.JobSource;
import de.sellfish.jobs.port.JobQuery;
import de.sellfish.jobs.port.RawJob;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests der Quellen mit Pflicht-Key/Config (Auth-Header, Skip ohne Config, Parsing).
 */
class KeyedJobSourcesTest {

    private final JobQuery query = new JobQuery(List.of("java"), "London", null, false, 10);

    private record Bound(RestClient.Builder builder, MockRestServiceServer server) {
    }

    private Bound bind() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        return new Bound(builder, server);
    }

    @Test
    void reedUsesBasicAuthAndParses() {
        Bound b = bind();
        b.server().expect(requestTo(Matchers.containsString("/search")))
                .andExpect(header("Authorization", Matchers.startsWith("Basic ")))
                .andRespond(withSuccess("""
                        {"results":[
                          {"jobId":1,"jobTitle":"Java Dev","employerName":"Acme","locationName":"London",
                           "jobUrl":"https://reed/1","jobDescription":"Spring",
                           "minimumSalary":50000,"maximumSalary":70000,"date":"01/06/2026"}
                        ]}
                        """, MediaType.APPLICATION_JSON));
        ReedSource source = new ReedSource(b.builder());

        List<RawJob> jobs = source.fetch(query, Map.of("api_key", "k"));
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).salaryRaw()).contains("50000");
        assertThat(jobs.get(0).postedAt()).isNotNull();
    }

    @Test
    void reedSkippedWithoutKey() {
        ReedSource source = new ReedSource(bind().builder());
        assertThat(source.fetch(query, Map.of())).isEmpty();
    }

    @Test
    void usaJobsSendsAuthHeadersAndParses() {
        Bound b = bind();
        b.server().expect(requestTo(Matchers.containsString("/search")))
                .andExpect(header("Authorization-Key", "secret"))
                .andRespond(withSuccess("""
                        {"SearchResult":{"SearchResultItems":[
                          {"MatchedObjectDescriptor":{
                             "PositionID":"X1","PositionTitle":"Java Specialist",
                             "OrganizationName":"Agency","PositionLocationDisplay":"DC",
                             "PositionURI":"https://usa/1","PublicationStartDate":"2026-06-01",
                             "UserArea":{"Details":{"JobSummary":"Spring"}}}}
                        ]}}
                        """, MediaType.APPLICATION_JSON));
        UsaJobsSource source = new UsaJobsSource(b.builder());

        List<RawJob> jobs = source.fetch(query, Map.of("api_key", "secret", "email", "me@example.com"));
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).title()).isEqualTo("Java Specialist");
        assertThat(jobs.get(0).company()).isEqualTo("Agency");
    }

    @Test
    void findworkUsesTokenAuth() {
        Bound b = bind();
        b.server().expect(requestTo(Matchers.containsString("/jobs/")))
                .andExpect(header("Authorization", "Token tok"))
                .andRespond(withSuccess("""
                        {"results":[
                          {"id":"f1","role":"Java Engineer","company_name":"Acme","location":"Remote",
                           "remote":true,"url":"https://fw/1","text":"Spring","date_posted":"2026-06-01T00:00:00Z"}
                        ]}
                        """, MediaType.APPLICATION_JSON));
        FindworkSource source = new FindworkSource(b.builder());

        List<RawJob> jobs = source.fetch(query, Map.of("api_key", "tok"));
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).remote()).isEqualTo("REMOTE");
    }

    @Test
    void jooblePostsToKeyedEndpoint() {
        Bound b = bind();
        b.server().expect(requestTo(Matchers.containsString("/api/mykey")))
                .andExpect(method(POST))
                .andRespond(withSuccess("""
                        {"jobs":[
                          {"id":"9","title":"Java Dev","company":"Acme","location":"London",
                           "link":"https://jb/9","snippet":"Spring","updated":"2026-06-01T10:00:00Z","salary":"£60k"}
                        ]}
                        """, MediaType.APPLICATION_JSON));
        JoobleSource source = new JoobleSource(b.builder());

        List<RawJob> jobs = source.fetch(query, Map.of("api_key", "mykey"));
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).title()).isEqualTo("Java Dev");
    }

    @Test
    void greenhousePerBoard() {
        Bound b = bind();
        b.server().expect(requestTo(Matchers.containsString("/boards/acme/jobs")))
                .andRespond(withSuccess("""
                        {"jobs":[
                          {"id":11,"title":"Java Engineer","location":{"name":"Remote"},
                           "absolute_url":"https://gh/11","content":"Spring","updated_at":"2026-06-01T10:00:00Z"}
                        ]}
                        """, MediaType.APPLICATION_JSON));
        GreenhouseSource source = new GreenhouseSource(b.builder());

        List<RawJob> jobs = source.fetch(query, Map.of("boards", "acme"));
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).externalRef()).isEqualTo("acme:11");
    }

    @Test
    void leverPerCompany() {
        Bound b = bind();
        b.server().expect(requestTo(Matchers.containsString("/postings/acme")))
                .andRespond(withSuccess("""
                        [
                          {"id":"L1","text":"Java Engineer","categories":{"location":"Berlin","commitment":"Full-time"},
                           "hostedUrl":"https://lv/1","descriptionPlain":"Spring","createdAt":1700000000000}
                        ]
                        """, MediaType.APPLICATION_JSON));
        LeverSource source = new LeverSource(b.builder());

        List<RawJob> jobs = source.fetch(query, Map.of("companies", "acme"));
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).title()).isEqualTo("Java Engineer");
        assertThat(jobs.get(0).location()).isEqualTo("Berlin");
        assertThat(jobs.get(0).postedAt()).isNotNull();
    }
}
