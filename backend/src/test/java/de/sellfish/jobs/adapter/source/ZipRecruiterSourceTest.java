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

class ZipRecruiterSourceTest {

    private final JobQuery query = new JobQuery(List.of("java"), "Berlin", null, false, 10);

    @Test
    void skipsWithoutApiKey() {
        RestClient.Builder builder = RestClient.builder();
        List<RawJob> jobs = new ZipRecruiterSource(builder).fetch(query, Map.of());
        assertThat(jobs).isEmpty();
    }

    @Test
    void parsesJobs() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        server.expect(requestTo(Matchers.containsString("search=java")))
                .andRespond(withSuccess(
                        """
                        {"jobs":[
                          {"id":"z1","name":"Java Dev","hiring_company":{"name":"Acme"},
                           "location":"Berlin","work_type":"full_time","snippet":"Spring",
                           "url":"https://ziprecruiter.com/z1","salary_minimum":"70000"}
                        ]}
                        """,
                        MediaType.APPLICATION_JSON));
        List<RawJob> jobs = new ZipRecruiterSource(builder).fetch(query, Map.of("api_key", "zk"));
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).title()).isEqualTo("Java Dev");
        assertThat(jobs.get(0).company()).isEqualTo("Acme");
        assertThat(jobs.get(0).salaryRaw()).isEqualTo("70000");
        server.verify();
    }
}
