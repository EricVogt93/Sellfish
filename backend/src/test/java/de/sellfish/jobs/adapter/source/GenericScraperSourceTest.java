package de.sellfish.jobs.adapter.source;
import de.sellfish.jobs.port.JobSource;
import de.sellfish.jobs.port.JobQuery;
import de.sellfish.jobs.port.RawJob;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GenericScraperSourceTest {

    private final GenericScraperSource source = new GenericScraperSource();

    private static final String HTML = """
            <html><body>
              <div class="job">
                <a class="title" href="/jobs/1">Java Entwickler</a>
                <span class="company">Acme GmbH</span>
                <span class="loc">Berlin</span>
              </div>
              <div class="job">
                <a class="title" href="/jobs/2">Python Entwickler</a>
                <span class="company">Beta AG</span>
                <span class="loc">München</span>
              </div>
              <div class="job">
                <span class="company">Ohne Titel</span>
              </div>
            </body></html>
            """;

    private Map<String, Object> config() {
        return Map.of(
                "item_selector", "div.job",
                "title_selector", "a.title",
                "company_selector", "span.company",
                "location_selector", "span.loc",
                "link_selector", "a.title",
                "link_base", "https://example.com");
    }

    @Test
    void parsesItemsWithSelectors() {
        List<RawJob> jobs = source.parse(Jsoup.parse(HTML), config());

        assertThat(jobs).hasSize(2); // drittes Item ohne Titel wird übersprungen
        RawJob first = jobs.get(0);
        assertThat(first.title()).isEqualTo("Java Entwickler");
        assertThat(first.company()).isEqualTo("Acme GmbH");
        assertThat(first.location()).isEqualTo("Berlin");
        assertThat(first.url()).isEqualTo("https://example.com/jobs/1");
        assertThat(first.sourceCode()).isEqualTo("SCRAPER");
    }

    @Test
    void fetchWithoutConfigReturnsEmpty() {
        assertThat(source.fetch(new JobQuery(List.of("x"), null, null, false, 5), Map.of())).isEmpty();
    }
}
