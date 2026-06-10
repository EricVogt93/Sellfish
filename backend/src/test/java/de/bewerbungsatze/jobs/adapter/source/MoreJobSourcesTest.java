package de.bewerbungsatze.jobs.adapter.source;

import de.bewerbungsatze.jobs.port.JobQuery;
import de.bewerbungsatze.jobs.port.RawJob;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MoreJobSourcesTest {

    private final JobQuery query = new JobQuery(List.of("java"), "Berlin", null, false, 10);

    private record Bound(RestClient.Builder builder, MockRestServiceServer server) {
    }

    private Bound bind() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        return new Bound(builder, server);
    }

    @Test
    void workingNomadsParsesAndFilters() {
        Bound b = bind();
        b.server().expect(requestTo(Matchers.containsString("/api/exposed_jobs/")))
                .andRespond(withSuccess("""
                        [
                          {"title":"Java Engineer","company_name":"Acme","location":"Anywhere",
                           "url":"https://wn/1","description":"Spring","pub_date":"2026-06-01T10:00:00Z"},
                          {"title":"Designer","company_name":"Beta","url":"https://wn/2","description":"Figma"}
                        ]
                        """, MediaType.APPLICATION_JSON));
        WorkingNomadsSource source = new WorkingNomadsSource(b.builder());

        List<RawJob> jobs = source.fetch(query, Map.of());
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).title()).isEqualTo("Java Engineer");
        assertThat(jobs.get(0).remote()).isEqualTo("REMOTE");
    }

    @Test
    void ashbyPerOrg() {
        Bound b = bind();
        b.server().expect(requestTo(Matchers.containsString("/job-board/acme")))
                .andRespond(withSuccess("""
                        {"jobs":[
                          {"id":"a1","title":"Java Engineer","location":"Remote","isRemote":true,
                           "jobUrl":"https://ashby/a1","descriptionPlain":"Spring","publishedAt":"2026-06-01T10:00:00Z"}
                        ]}
                        """, MediaType.APPLICATION_JSON));
        AshbySource source = new AshbySource(b.builder());

        List<RawJob> jobs = source.fetch(query, Map.of("orgs", "acme"));
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).externalRef()).isEqualTo("acme:a1");
        assertThat(jobs.get(0).remote()).isEqualTo("REMOTE");
    }

    @Test
    void recruiteePerCompany() {
        Bound b = bind();
        b.server().expect(requestTo(Matchers.containsString("recruitee.com/api/offers/")))
                .andRespond(withSuccess("""
                        {"offers":[
                          {"id":7,"title":"Java Developer","location":"Berlin","careers_url":"https://rc/7",
                           "description":"Spring","published_at":"2026-06-01T10:00:00Z"}
                        ]}
                        """, MediaType.APPLICATION_JSON));
        RecruiteeSource source = new RecruiteeSource(b.builder());

        List<RawJob> jobs = source.fetch(query, Map.of("companies", "acme"));
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).title()).isEqualTo("Java Developer");
        assertThat(jobs.get(0).location()).isEqualTo("Berlin");
    }

    @Test
    void smartRecruitersPerCompany() {
        Bound b = bind();
        b.server().expect(requestTo(Matchers.containsString("/companies/acme/postings")))
                .andRespond(withSuccess("""
                        {"content":[
                          {"id":"s1","name":"Java Engineer","location":{"city":"Berlin","country":"de","remote":false},
                           "releasedDate":"2026-06-01T10:00:00Z"}
                        ]}
                        """, MediaType.APPLICATION_JSON));
        SmartRecruitersSource source = new SmartRecruitersSource(b.builder());

        List<RawJob> jobs = source.fetch(query, Map.of("companies", "acme"));
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).title()).isEqualTo("Java Engineer");
        assertThat(jobs.get(0).url()).contains("smartrecruiters.com/acme/s1");
    }

    @Test
    void atsSourcesSkippedWithoutConfig() {
        Bound b = bind();
        assertThat(new AshbySource(b.builder()).fetch(query, Map.of())).isEmpty();
        assertThat(new RecruiteeSource(b.builder()).fetch(query, Map.of())).isEmpty();
        assertThat(new SmartRecruitersSource(b.builder()).fetch(query, Map.of())).isEmpty();
    }
}
