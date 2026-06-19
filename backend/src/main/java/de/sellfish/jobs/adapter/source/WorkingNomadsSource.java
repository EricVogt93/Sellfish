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
 * Working-Nomads-API (kostenlos, without Key, international/remote). Filterung clientseitig.
 */
@Component
public class WorkingNomadsSource implements JobSource {

    public static final String CODE = "WORKINGNOMADS";
    private static final String BASE_URL = "https://www.workingnomads.com";
    private static final Logger log = LoggerFactory.getLogger(WorkingNomadsSource.class);

    private final RestClient client;

    public WorkingNomadsSource(RestClient.Builder builder) {
        this.client = builder.baseUrl(BASE_URL).build();
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<RawJob> fetch(JobQuery query, Map<String, Object> config) {
        try {
            JsonNode response =
                    client.get().uri("/api/exposed_jobs/").retrieve().body(JsonNode.class);
            if (response == null || !response.isArray()) {
                return List.of();
            }
            List<RawJob> jobs = new ArrayList<>();
            for (JsonNode item : response) {
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
            log.warn("Working-Nomads-Abruf failed: {}", e.getMessage());
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
        return new RawJob(
                CODE,
                JobSourceSupport.firstText(item, "url", "title"),
                JobSourceSupport.text(item, "title"),
                JobSourceSupport.text(item, "company_name"),
                JobSourceSupport.text(item, "location"),
                "REMOTE",
                JobSourceSupport.text(item, "description"),
                JobSourceSupport.text(item, "url"),
                null,
                JobSourceSupport.parseIso(JobSourceSupport.text(item, "pub_date")),
                item.toString());
    }
}
