package de.bewerbungsatze.jobs.source;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generischer, per Konfiguration steuerbarer HTML-Scraper (CSS-Selektoren). Nur für Portale
 * einsetzen, deren Nutzungsbedingungen Scraping erlauben.
 * <p>
 * Erwartete Config-Schlüssel: {@code search_url} (mit Platzhaltern {query}/{location}),
 * {@code item_selector}, {@code title_selector}, {@code company_selector},
 * {@code location_selector}, {@code link_selector}, {@code link_base}.
 */
@Component
public class GenericScraperSource implements JobSource {

    public static final String CODE = "SCRAPER";
    private static final Logger log = LoggerFactory.getLogger(GenericScraperSource.class);

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<RawJob> fetch(JobQuery query, Map<String, Object> config) {
        String searchUrl = str(config, "search_url");
        String itemSelector = str(config, "item_selector");
        if (searchUrl == null || itemSelector == null) {
            log.warn("Scraper ohne search_url/item_selector – übersprungen");
            return List.of();
        }
        String url = searchUrl
                .replace("{query}", urlEncode(query.keywordString()))
                .replace("{location}", urlEncode(query.location() == null ? "" : query.location()));
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; BewerbungsatzeBot/1.0)")
                    .timeout(15000)
                    .get();
            return parse(doc, config);
        } catch (Exception e) {
            log.warn("Scraper-Abruf fehlgeschlagen ({}): {}", url, e.getMessage());
            return List.of();
        }
    }

    /** Reine, netzwerkfreie Parse-Logik – getrennt für Testbarkeit. */
    public List<RawJob> parse(Document doc, Map<String, Object> config) {
        String itemSelector = str(config, "item_selector");
        String titleSelector = str(config, "title_selector");
        String companySelector = str(config, "company_selector");
        String locationSelector = str(config, "location_selector");
        String linkSelector = str(config, "link_selector");
        String linkBase = str(config, "link_base");

        List<RawJob> jobs = new ArrayList<>();
        Elements items = doc.select(itemSelector);
        for (Element item : items) {
            String title = text(item, titleSelector);
            if (title == null || title.isBlank()) {
                continue;
            }
            String link = link(item, linkSelector, linkBase);
            jobs.add(new RawJob(
                    CODE,
                    link,
                    title,
                    text(item, companySelector),
                    text(item, locationSelector),
                    null,
                    null,
                    link,
                    null,
                    null,
                    null));
        }
        return jobs;
    }

    private String text(Element item, String selector) {
        if (selector == null) {
            return null;
        }
        Element el = item.selectFirst(selector);
        return el == null ? null : el.text().strip();
    }

    private String link(Element item, String selector, String base) {
        if (selector == null) {
            return null;
        }
        Element el = item.selectFirst(selector);
        if (el == null) {
            return null;
        }
        String href = el.hasAttr("href") ? el.attr("href") : el.text();
        if (href == null || href.isBlank()) {
            return null;
        }
        if (base != null && href.startsWith("/")) {
            return base.replaceAll("/+$", "") + href;
        }
        return href;
    }

    private String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }

    private String str(Map<String, Object> config, String key) {
        Object v = config.get(key);
        return v == null ? null : v.toString();
    }
}
