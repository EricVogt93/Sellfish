package de.sellfish.jobs.adapter.source;

import com.fasterxml.jackson.databind.JsonNode;
import de.sellfish.jobs.port.JobQuery;
import de.sellfish.jobs.port.JobSource;
import de.sellfish.jobs.port.RawJob;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Arbeitnow Job-Board-API (kostenlos, without Key, Europa/Remote). Keine serverseitige Suche,
 * daher Filterung nach Suchbegriffen clientseitig.
 */
@Component
public class ArbeitnowSource implements JobSource {

    public static final String CODE = "ARBEITNOW";
    private static final String BASE_URL = "https://www.arbeitnow.com/api";
    private static final Logger log = LoggerFactory.getLogger(ArbeitnowSource.class);

    private final RestClient client;

    public ArbeitnowSource(RestClient.Builder builder) {
        this.client = builder.baseUrl(BASE_URL).build();
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<RawJob> fetch(JobQuery query, Map<String, Object> config) {
        try {
            JsonNode response = client.get().uri("/job-board-api").retrieve().body(JsonNode.class);
            if (response == null) {
                return List.of();
            }
            List<RawJob> jobs = new ArrayList<>();
            for (JsonNode item : response.path("data")) {
                RawJob job = toRawJob(item);
                if (matches(job, query)) {
                    jobs.add(job);
                }
                if (jobs.size() >= query.size()) {
                    break;
                }
            }
            return jobs;
        } catch (RestClientException e) {
            log.warn("Arbeitnow-Abruf failed: {}", e.getMessage());
            return List.of();
        }
    }

    private boolean matches(RawJob job, JobQuery query) {
        if (query.keywords() == null || query.keywords().isEmpty()) {
            return true;
        }
        String haystack =
                (job.title() + " " + (job.description() == null ? "" : job.description())).toLowerCase(Locale.ROOT);
        return query.keywords().stream().anyMatch(k -> haystack.contains(k.toLowerCase(Locale.ROOT)));
    }

    private RawJob toRawJob(JsonNode item) {
        boolean remote = item.path("remote").asBoolean(false);
        return new RawJob(
                CODE,
                JobSourceSupport.text(item, "slug"),
                JobSourceSupport.firstText(item, "title"),
                JobSourceSupport.text(item, "company_name"),
                JobSourceSupport.text(item, "location"),
                remote ? "REMOTE" : null,
                JobSourceSupport.text(item, "description"),
                JobSourceSupport.text(item, "url"),
                null,
                JobSourceSupport.parseEpochSeconds(item, "created_at"),
                item.toString());
    }
}
