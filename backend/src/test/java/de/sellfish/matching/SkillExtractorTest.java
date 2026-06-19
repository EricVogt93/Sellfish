package de.sellfish.matching;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class SkillExtractorTest {

    @Test
    void extractsKnownSkillsCaseInsensitive() {
        Set<String> skills = SkillExtractor.extract(
                "We seek a Java and Spring developer with React and PostgreSQL experience. AWS is a plus.");
        assertThat(skills).contains("java", "spring", "react", "postgresql", "aws");
    }

    @Test
    void emptyOrNullTextReturnsEmpty() {
        assertThat(SkillExtractor.extract(null)).isEmpty();
        assertThat(SkillExtractor.extract("")).isEmpty();
        assertThat(SkillExtractor.extract("   ")).isEmpty();
    }

    @Test
    void noSkillsReturnsEmpty() {
        assertThat(SkillExtractor.extract("We hire a person to do things in an office."))
                .isEmpty();
    }

    @Test
    void overlapFullMatchReturnsOne() {
        String text = "Java, Spring, React, PostgreSQL";
        assertThat(SkillExtractor.overlap(text, text)).isEqualTo(1.0);
    }

    @Test
    void overlapPartialMatch() {
        String job = "Java Spring React PostgreSQL Docker";
        String profile = "Java Spring Python AWS";
        // job skills: java, spring, react, postgresql, docker (5)
        // intersection: java, spring (2) -> 2/5 = 0.4
        assertThat(SkillExtractor.overlap(job, profile)).isEqualTo(0.4);
    }

    @Test
    void overlapNoJobSkillsReturnsZero() {
        assertThat(SkillExtractor.overlap("office work", "Java Spring")).isEqualTo(0.0);
    }

    @Test
    void overlapNoProfileSkillsReturnsZero() {
        assertThat(SkillExtractor.overlap("Java Spring", "office work")).isEqualTo(0.0);
    }

    @Test
    void deduplicatesRepeatedSkills() {
        Set<String> skills = SkillExtractor.extract("Java java JAVA Python python");
        assertThat(skills).containsExactlyInAnyOrder("java", "python");
    }
}
