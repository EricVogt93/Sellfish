package de.bewerbungsatze.jobs.adapter.source;
import de.bewerbungsatze.jobs.port.JobSource;
import de.bewerbungsatze.jobs.port.JobQuery;
import de.bewerbungsatze.jobs.port.RawJob;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Stellen aus der (kostenlosen) Jobsuche-API der Bundesagentur für Arbeit.
 * Authentifizierung via statischem Client-Key im Header {@code X-API-Key}.
 */
@Component
public class BundesagenturSource implements JobSource {

    public static final String CODE = "BA";
    private static final String BASE_URL = "https://rest.arbeitsagentur.de/jobboerse/jobsuche-service";
    private static final String API_KEY = "jobboerse-jobsuche";

    private static final Logger log = LoggerFactory.getLogger(BundesagenturSource.class);

    private final RestClient client;

    public BundesagenturSource(RestClient.Builder builder) {
        this.client = builder
                .baseUrl(BASE_URL)
                .defaultHeader("X-API-Key", API_KEY)
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
                    .uri(uri -> {
                        var b = uri.path("/pc/v4/app/jobs")
                                .queryParam("page", 1)
                                .queryParam("size", Math.max(1, query.size()))
                                .queryParam("angebotsart", 1); // 1 = Arbeit/Anstellung
                        if (!query.keywordString().isBlank()) {
                            b.queryParam("was", query.keywordString());
                        }
                        if (query.location() != null && !query.location().isBlank()) {
                            b.queryParam("wo", query.location());
                        }
                        if (query.radiusKm() != null) {
                            b.queryParam("umkreis", query.radiusKm());
                        }
                        return b.build();
                    })
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                return List.of();
            }
            List<RawJob> jobs = new ArrayList<>();
            for (JsonNode item : response.path("stellenangebote")) {
                jobs.add(toRawJob(item));
            }
            return jobs;
        } catch (RestClientException e) {
            log.warn("Bundesagentur-Abruf fehlgeschlagen: {}", e.getMessage());
            return List.of();
        }
    }

    private RawJob toRawJob(JsonNode item) {
        String refnr = item.path("refnr").asText(null);
        JsonNode ort = item.path("arbeitsort");
        String location = joinLocation(
                ort.path("plz").asText(""), ort.path("ort").asText(""), ort.path("region").asText(""));

        String description = fetchDescription(refnr);

        return new RawJob(
                CODE,
                refnr,
                item.path("titel").asText(item.path("beruf").asText("Stelle")),
                item.path("arbeitgeber").asText(null),
                location.isBlank() ? null : location,
                null,
                description,
                buildUrl(refnr),
                null,
                parseDate(item.path("aktuelleVeroeffentlichungsdatum").asText(null)),
                item.toString());
    }

    private String fetchDescription(String refnr) {
        if (refnr == null) {
            return null;
        }
        try {
            String encoded = Base64.getEncoder().encodeToString(refnr.getBytes(StandardCharsets.UTF_8));
            JsonNode detail = client.get()
                    .uri("/pc/v4/jobdetails/{ref}", encoded)
                    .retrieve()
                    .body(JsonNode.class);
            return detail == null ? null : detail.path("stellenbeschreibung").asText(null);
        } catch (RestClientException e) {
            return null;
        }
    }

    private String buildUrl(String refnr) {
        return refnr == null ? null
                : "https://www.arbeitsagentur.de/jobsuche/jobdetail/" + refnr;
    }

    private String joinLocation(String plz, String ort, String region) {
        StringBuilder sb = new StringBuilder();
        if (!ort.isBlank()) {
            if (!plz.isBlank()) {
                sb.append(plz).append(' ');
            }
            sb.append(ort);
        } else if (!region.isBlank()) {
            sb.append(region);
        }
        return sb.toString().trim();
    }

    private java.time.Instant parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
