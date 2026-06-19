package de.sellfish.docs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TextExtractionServiceTest {

    private final TextExtractionService service = new TextExtractionService();

    @Test
    void extractsPlainText() {
        assertThat(service.extract("Hello Sellfish world".getBytes())).contains("Hello Sellfish world");
    }

    @Test
    void extractsHtmlStripped() {
        String result = service.extract("<html><body><p>Java Developer</p></body></html>".getBytes());
        assertThat(result).contains("Java Developer");
        assertThat(result).doesNotContain("<p>");
    }

    @Test
    void emptyInputReturnsEmpty() {
        assertThat(service.extract("   ".getBytes())).isEmpty();
    }

    @Test
    void throwsOnNullInput() {
        assertThatThrownBy(() -> service.extract(null)).isInstanceOf(Exception.class);
    }
}
