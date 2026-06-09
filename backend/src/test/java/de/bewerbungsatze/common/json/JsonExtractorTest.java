package de.bewerbungsatze.common.json;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonExtractorTest {

    @Test
    void extractsPlainObject() {
        assertThat(JsonExtractor.extract("{\"a\":1}")).isEqualTo("{\"a\":1}");
    }

    @Test
    void stripsMarkdownFences() {
        String raw = "```json\n{\"a\":1,\"b\":[2,3]}\n```";
        assertThat(JsonExtractor.extract(raw)).isEqualTo("{\"a\":1,\"b\":[2,3]}");
    }

    @Test
    void extractsObjectFromSurroundingText() {
        String raw = "Hier ist das Ergebnis: {\"name\":\"Eric\"} – fertig.";
        assertThat(JsonExtractor.extract(raw)).isEqualTo("{\"name\":\"Eric\"}");
    }

    @Test
    void extractsArray() {
        String raw = "Antwort:\n[{\"x\":1},{\"y\":2}]";
        assertThat(JsonExtractor.extract(raw)).isEqualTo("[{\"x\":1},{\"y\":2}]");
    }

    @Test
    void ignoresBracesInsideStrings() {
        String raw = "{\"text\":\"a } b { c\"}";
        assertThat(JsonExtractor.extract(raw)).isEqualTo(raw);
    }

    @Test
    void handlesNull() {
        assertThat(JsonExtractor.extract(null)).isNull();
    }
}
