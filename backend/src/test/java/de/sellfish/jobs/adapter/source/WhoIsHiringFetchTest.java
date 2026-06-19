package de.sellfish.jobs.adapter.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import de.sellfish.jobs.port.JobQuery;
import de.sellfish.jobs.port.RawJob;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class WhoIsHiringFetchTest {

    private final JobQuery query = new JobQuery(List.of("java"), null, null, false, 10);

    @Test
    void fetchParsesParagraphsFromHnThread() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();

        // maxitem -> a single id; the loop checks that item
        server.expect(requestTo(containsString("/maxitem.json")))
                .andRespond(withSuccess("100", MediaType.APPLICATION_JSON));
        // the item is the Who Is Hiring thread with two paragraphs
        server.expect(requestTo(containsString("/item/100.json")))
                .andRespond(withSuccess(
                        """
                        {"id":100,"title":"Ask HN: Who is hiring (June 2026)?",
                         "text":"<p>Senior Java Engineer at Acme (Berlin, remote) https://acme.com/job</p><p>Sales person in Munich</p>"}
                        """,
                        MediaType.APPLICATION_JSON));
        // the thread itself is fetched again for its text
        server.expect(requestTo(containsString("/item/100.json")))
                .andRespond(withSuccess(
                        """
                        {"id":100,"title":"Ask HN: Who is hiring (June 2026)?",
                         "text":"<p>Senior Java Engineer at Acme (Berlin, remote) https://acme.com/job</p><p>Sales person in Munich</p>"}
                        """,
                        MediaType.APPLICATION_JSON));

        List<RawJob> jobs = new WhoIsHiringSource(builder).fetch(query, Map.of());

        // "Sales person" has no "java" -> filtered; the Java job remains
        assertThat(jobs).isNotEmpty();
        RawJob first = jobs.get(0);
        assertThat(first.title()).contains("Java");
        assertThat(first.company()).isEqualTo("Acme");
        assertThat(first.remote()).isEqualTo("REMOTE");
        assertThat(first.url()).contains("acme.com");
    }

    @Test
    void fetchReturnsEmptyWhenMaxItemFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        server.expect(requestTo(containsString("/maxitem.json")))
                .andRespond(withSuccess("not a number", MediaType.APPLICATION_JSON));
        // search loop items may 404 / error -> returns empty overall
        List<RawJob> jobs = new WhoIsHiringSource(builder).fetch(query, Map.of());
        assertThat(jobs).isEmpty();
    }
}
