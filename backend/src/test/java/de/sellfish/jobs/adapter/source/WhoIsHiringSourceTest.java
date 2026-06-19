package de.sellfish.jobs.adapter.source;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WhoIsHiringSourceTest {

    @Test
    void htmlToTextStripsTagsEntitiesAndCollapsesWhitespace() {
        String out = WhoIsHiringSource.htmlToText("<p>Java <b>Dev</b> &amp; more</p>");
        assertThat(out).isEqualTo("Java Dev more");
    }

    @Test
    void htmlToTextHandlesNestedTags() {
        assertThat(WhoIsHiringSource.htmlToText("<div><span>A</span> <em>B</em></div>"))
                .isEqualTo("A B");
    }

    @Test
    void htmlToTextCollapsesMultipleSpaces() {
        assertThat(WhoIsHiringSource.htmlToText("a    b\n\tc")).isEqualTo("a b c");
    }

    @Test
    void codeConstantMatchesCountryMap() {
        // Who Is Hiring is worldwide remote in SourceCountries
        assertThat(SourceCountries.isRemote("WHOWHIRING")).isTrue();
    }
}
