package de.sellfish.jobs.adapter.source;

import com.fasterxml.jackson.databind.JsonNode;
import de.sellfish.common.text.Strings;
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
 * We Work Remotely — feed (keyless, JSON-ähnlich via HTML-Parse).
 * Nutzt den /api/jobs Endpunkt als RSS-Ersatz.
 */
@Component
public class WeWorkRemotelySource implements JobSource {

    public static final String CODE = "WWREMOTE";
    private static final String BASE_URL = "https://weworkremotely.com";
    private static final Logger log = LoggerFactory.getLogger(WeWorkRemotelySource.class);

    private final RestClient client;

    public WeWorkRemotelySource(RestClient.Builder builder) {
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
            JsonNode response = client.get().uri("/api/jobs").retrieve().body(JsonNode.class);
            if (response == null || !response.isArray()) return List.of();
            List<RawJob> jobs = new ArrayList<>();
            for (JsonNode item : response) {
                RawJob job = toRawJob(item);
                if (matches(job, query)) jobs.add(job);
                if (jobs.size() >= query.size()) break;
            }
            return jobs;
        } catch (RestClientException e) {
            log.warn("WWR-Abruf failed: {}", e.getMessage());
            return List.of();
        }
    }

    private boolean matches(RawJob job, JobQuery query) {
        if (query.keywords() == null || query.keywords().isEmpty()) return true;
        String haystack = (job.title() + " " + Strings.nz(job.company()) + " " + Strings.nz(job.description()))
                .toLowerCase(Locale.ROOT);
        return query.keywords().stream().anyMatch(k -> haystack.contains(k.toLowerCase(Locale.ROOT)));
    }

    private RawJob toRawJob(JsonNode item) {
        return new RawJob(
                CODE,
                JobSourceSupport.text(item, "id"),
                JobSourceSupport.text(item, "title"),
                JobSourceSupport.text(item, "company_name"),
                JobSourceSupport.text(item, "region"),
                "REMOTE",
                JobSourceSupport.text(item, "description"),
                jobUrl(item),
                null,
                JobSourceSupport.parseIso(JobSourceSupport.text(item, "published_at")),
                item.toString());
    }

    private String jobUrl(JsonNode item) {
        String id = JobSourceSupport.text(item, "id");
        String slug = JobSourceSupport.text(item, "slug");
        if (id != null && slug != null) return BASE_URL + "/jobs/" + id + "/" + slug;
        String url = JobSourceSupport.text(item, "url");
        return url != null && url.startsWith("/") ? BASE_URL + url : url;
    }
}
