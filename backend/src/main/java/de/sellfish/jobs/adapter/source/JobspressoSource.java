package de.sellfish.jobs.adapter.source;

import com.fasterxml.jackson.databind.JsonNode;
import de.sellfish.common.text.Strings;
import de.sellfish.jobs.port.JobQuery;
import de.sellfish.jobs.port.JobSource;
import de.sellfish.jobs.port.RawJob;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Jobspresso (remote jobs, keyless, curated).
 */
@Component
public class JobspressoSource implements JobSource {

    public static final String CODE = "JOBSPRESSO";
    private static final String BASE_URL = "https://jobspresso.co";
    private static final Logger log = LoggerFactory.getLogger(JobspressoSource.class);

    private final RestClient client;

    public JobspressoSource(RestClient.Builder builder) {
        this.client = builder.baseUrl(BASE_URL)
                .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; SellfishBot/1.0)")
                .build();
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<RawJob> fetch(JobQuery query, Map<String, Object> config) {
        try {
            JsonNode response = client.get()
                    .uri("/wp-json/wp/v2/job-listings?per_page=" + Math.min(100, query.size()))
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.isArray()) return List.of();
            List<RawJob> jobs = new ArrayList<>();
            for (JsonNode item : response) {
                RawJob job = toRawJob(item);
                if (matches(job, query)) jobs.add(job);
                if (jobs.size() >= query.size()) break;
            }
            return jobs;
        } catch (RestClientException e) {
            log.warn("Jobspresso-Abruf failed: {}", e.getMessage());
            return List.of();
        }
    }

    private boolean matches(RawJob job, JobQuery query) {
        if (query.keywords() == null || query.keywords().isEmpty()) return true;
        String haystack = (job.title() + " " + Strings.nz(job.description())).toLowerCase(java.util.Locale.ROOT);
        return query.keywords().stream().anyMatch(k -> haystack.contains(k.toLowerCase(java.util.Locale.ROOT)));
    }

    private RawJob toRawJob(JsonNode item) {
        String desc = JobSourceSupport.text(item.path("content"), "rendered");
        String title = JobSourceSupport.text(item.path("title"), "rendered");
        return new RawJob(
                CODE,
                JobSourceSupport.text(item, "slug"),
                title != null ? title : JobSourceSupport.text(item, "title"),
                JobSourceSupport.text(item.path("meta"), "company"),
                JobSourceSupport.text(item.path("meta"), "location"),
                "REMOTE",
                desc != null ? htmlToText(desc) : null,
                JobSourceSupport.text(item, "link"),
                null,
                JobSourceSupport.parseIso(JobSourceSupport.text(item, "date")),
                item.toString());
    }

    static String htmlToText(String html) {
        return html.replaceAll("<[^>]+>", " ")
                .replaceAll("&[a-z]+;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
