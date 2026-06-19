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

class RemoteBoardSourcesTest {

    private final JobQuery query = new JobQuery(List.of("java"), null, null, false, 10);

    private record Bound(RestClient.Builder builder, MockRestServiceServer server) {}

    private Bound bind() {
        RestClient.Builder builder = RestClient.builder();
        return new Bound(
                builder,
                MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build());
    }

    @Test
    void jobspressoParsesWpArrayAndStripsHtml() {
        Bound b = bind();
        b.server()
                .expect(requestTo(Matchers.containsString("/wp-json/wp/v2/job-listings")))
                .andRespond(withSuccess(
                        """
                        [{"slug":"java-dev","title":{"rendered":"Java Engineer"},
                          "content":{"rendered":"<p>Spring <strong>boot</strong></p>"},
                          "meta":{"company":"Acme","location":"Remote"},"link":"https://jobspresso.co/j/1","date":"2026-06-01T10:00:00"}]
                        """,
                        MediaType.APPLICATION_JSON));
        List<RawJob> jobs = new JobspressoSource(b.builder()).fetch(query, Map.of());
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).title()).isEqualTo("Java Engineer");
        assertThat(jobs.get(0).company()).isEqualTo("Acme");
        assertThat(jobs.get(0).description()).doesNotContain("<p>").contains("Spring");
    }

    @Test
    void jobspressoHtmlToTextStripsTagsAndEntities() {
        assertThat(JobspressoSource.htmlToText("<p>Hello &amp; world</p>")).isEqualTo("Hello world");
    }

    @Test
    void remoteCoParsesCards() {
        Bound b = bind();
        b.server()
                .expect(requestTo(Matchers.containsString("/remote-jobs/search")))
                .andRespond(withSuccess(
                        """
                        [{"card":{"id":"r1","job_title":"Java Dev","company_name":"Acme",
                         "location":"Anywhere","description":"Spring","url":"https://remote.co/r1","date":"2026-06-01T00:00:00Z"}},
                         {"nocard":true}]
                        """,
                        MediaType.APPLICATION_JSON));
        List<RawJob> jobs = new RemoteCoSource(b.builder()).fetch(query, Map.of());
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).title()).isEqualTo("Java Dev");
        assertThat(jobs.get(0).company()).isEqualTo("Acme");
    }

    @Test
    void remoteCoReturnsEmptyWhenNotArray() {
        Bound b = bind();
        b.server()
                .expect(requestTo(Matchers.containsString("/remote-jobs/search")))
                .andRespond(withSuccess(
                        """
                        {"error":"nope"}
                        """,
                        MediaType.APPLICATION_JSON));
        assertThat(new RemoteCoSource(b.builder()).fetch(query, Map.of())).isEmpty();
    }

    @Test
    void justRemoteParsesJobsObject() {
        Bound b = bind();
        b.server()
                .expect(requestTo(Matchers.containsString("/jobs")))
                .andRespond(withSuccess(
                        """
                        {"jobs":[
                          {"id":"j1","title":"Java Backend","company":"Acme","location":"Worldwide",
                           "description":"Spring Kafka","url":"https://justremote.co/j1","postedAt":"2026-06-01T00:00:00Z"},
                          {"id":"j2","title":"Designer","company":"Beta","description":"Figma","url":"https://x"}
                        ]}
                        """,
                        MediaType.APPLICATION_JSON));
        List<RawJob> jobs = new JustRemoteSource(b.builder()).fetch(query, Map.of());
        assertThat(jobs).hasSize(1); // Designer filtered out (no "java")
        assertThat(jobs.get(0).company()).isEqualTo("Acme");
    }
}
