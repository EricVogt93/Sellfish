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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Who Is Hiring — Hacker News Monthly Thread (der aktuelle, gepinnte Who-Is-Hiring-Post).
 * Parsed den HTML-Text clientseitig mit Regex nach Job-Einträgen.
 */
@Component
public class WhoIsHiringSource implements JobSource {

    public static final String CODE = "WHOWHIRING";
    private static final String HN_API = "https://hacker-news.firebaseio.com/v0";
    private static final Logger log = LoggerFactory.getLogger(WhoIsHiringSource.class);

    private static final Pattern JOB_PATTERN =
            Pattern.compile("<p>(.*?)</p>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private final RestClient client;

    public WhoIsHiringSource(RestClient.Builder builder) {
        this.client = builder.defaultHeader("User-Agent", "Mozilla/5.0 (compatible; SellfishBot/1.0)")
                .build();
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<RawJob> fetch(JobQuery query, Map<String, Object> config) {
        try {
            Long itemId = findWhoIsHiringId();
            if (itemId == null) return List.of();

            JsonNode item = client.get()
                    .uri(HN_API + "/item/{id}.json", itemId)
                    .retrieve()
                    .body(JsonNode.class);
            if (item == null) return List.of();

            String text = item.path("text").asText("");
            if (text.isBlank()) return List.of();

            List<RawJob> jobs = new ArrayList<>();
            Matcher m = JOB_PATTERN.matcher(text);
            while (m.find()) {
                String p = htmlToText(m.group(1));
                if (p.length() < 40) continue;
                RawJob job = parseJobFromParagraph(p);
                if (job != null && matches(job, query)) {
                    jobs.add(job);
                    if (jobs.size() >= query.size()) break;
                }
            }
            return jobs;
        } catch (RestClientException e) {
            log.warn("WhoIsHiring-Abruf failed: {}", e.getMessage());
            return List.of();
        }
    }

    private Long findWhoIsHiringId() {
        try {
            JsonNode maxItem =
                    client.get().uri(HN_API + "/maxitem.json").retrieve().body(JsonNode.class);
            long maxId = maxItem.asLong();
            for (int i = 0; i < 50; i++) {
                JsonNode item = client.get()
                        .uri(HN_API + "/item/{id}.json", maxId - i)
                        .retrieve()
                        .body(JsonNode.class);
                if (item == null) continue;
                String title = item.path("title").asText("").toLowerCase(Locale.ROOT);
                if (title.contains("who is hiring") && item.path("dead").asBoolean(false) == false) {
                    return item.path("id").asLong();
                }
            }
        } catch (RestClientException e) {
            log.warn("HN-Suche failed: {}", e.getMessage());
        }
        return null;
    }

    private RawJob parseJobFromParagraph(String text) {
        String location = extractLocation(text);
        boolean remote = text.toLowerCase(Locale.ROOT).contains("remote");
        String url = extractUrl(text);
        return new RawJob(
                CODE,
                Integer.toHexString(text.hashCode()),
                extractTitle(text),
                extractCompany(text),
                location,
                remote ? "REMOTE" : null,
                text,
                url,
                null,
                null,
                text);
    }

    private String extractTitle(String text) {
        int end = Math.min(text.length(), 80);
        String first = text.substring(0, end);
        int sep = first.indexOf(" | ");
        if (sep >= 0) return first.substring(0, sep).trim();
        sep = first.indexOf(" at ");
        if (sep >= 0) return first.substring(0, sep).trim();
        sep = first.indexOf(", ");
        if (sep >= 10) return first.substring(0, sep).trim();
        return first.trim();
    }

    private String extractCompany(String text) {
        java.util.regex.Matcher m = Pattern.compile("\\b(?:at|@)\\s+([A-Z][A-Za-z0-9&.\\- ]{2,40})\\b")
                .matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private String extractLocation(String text) {
        java.util.regex.Matcher m = Pattern.compile("\\b([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)?(?:,\\s*[A-Z]{2})?)\\b"
                        + "(?!\\s+(?:at|is|with|for|and|or|the|a|in))")
                .matcher(text);
        List<String> locs = new ArrayList<>();
        while (m.find()) {
            String loc = m.group(1);
            if (loc.length() >= 4 && !isStopword(loc)) locs.add(loc);
        }
        return locs.isEmpty() ? null : String.join(", ", locs.subList(0, Math.min(3, locs.size())));
    }

    private String extractUrl(String text) {
        java.util.regex.Matcher m = Pattern.compile("(https?://[^\\s)]+)").matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private boolean isStopword(String s) {
        return Set.of(
                        "This", "That", "These", "There", "They", "With", "From", "About", "After", "While", "Would",
                        "Could", "Should", "Please", "Apply", "Seeking")
                .contains(s);
    }

    private boolean matches(RawJob job, JobQuery query) {
        if (query.keywords() == null || query.keywords().isEmpty()) return true;
        String haystack = (job.title() + " " + Strings.nz(job.description())).toLowerCase(Locale.ROOT);
        return query.keywords().stream().anyMatch(k -> haystack.contains(k.toLowerCase(Locale.ROOT)));
    }

    static String htmlToText(String html) {
        return html.replaceAll("<[^>]+>", " ")
                .replaceAll("&[a-z]+;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
