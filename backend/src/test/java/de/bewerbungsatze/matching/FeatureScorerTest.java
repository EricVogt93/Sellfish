package de.bewerbungsatze.matching;

import de.bewerbungsatze.jobs.Job;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class FeatureScorerTest {

    private final FeatureScorer scorer = new FeatureScorer(null);

    private Job job(String title, String company, String location, String description) {
        Job j = new Job("BA", "fp-" + title, title);
        j.setCompany(company);
        j.setLocation(location);
        j.setDescription(description);
        j.setPostedAt(Instant.now());
        return j;
    }

    private MatchContext ctx(List<String> titles, List<String> keywords, String location,
                             String remotePref, Set<String> excluded) {
        return new MatchContext(titles, keywords, location, remotePref, excluded, "");
    }

    @Test
    void titleMatchesDesiredTitle() {
        Features f = scorer.score(
                job("Senior Java Entwickler", "Acme", "Berlin", ""),
                ctx(List.of("Java Entwickler"), List.of(), null, "ANY", Set.of()), 0.0);
        assertThat(f.title()).isEqualTo(1.0);
    }

    @Test
    void keywordScoreCountsMatches() {
        Features f = scorer.score(
                job("Backend Engineer", "Acme", "Berlin", "Wir nutzen Spring und Kafka"),
                ctx(List.of(), List.of("spring", "kafka", "rust"), null, "ANY", Set.of()), 0.0);
        assertThat(f.keyword()).isCloseTo(2.0 / 3.0, within(1e-9));
    }

    @Test
    void locationMatchesToken() {
        Features f = scorer.score(
                job("Dev", "Acme", "10115 Berlin", ""),
                ctx(List.of(), List.of(), "Berlin", "ONSITE", Set.of()), 0.0);
        assertThat(f.location()).isEqualTo(1.0);
    }

    @Test
    void remotePreferenceRewardsRemoteJobs() {
        Features remote = scorer.score(
                job("Dev", "Acme", "Berlin", "100% Remote / Homeoffice"),
                ctx(List.of(), List.of(), null, "REMOTE", Set.of()), 0.0);
        Features onsite = scorer.score(
                job("Dev", "Acme", "Berlin", "Vor Ort im Büro"),
                ctx(List.of(), List.of(), null, "REMOTE", Set.of()), 0.0);
        assertThat(remote.remote()).isGreaterThan(onsite.remote());
    }

    @Test
    void recencyDecaysOverTime() {
        Job old = job("Dev", "Acme", "Berlin", "");
        old.setPostedAt(Instant.now().minus(30, ChronoUnit.DAYS));
        Features f = scorer.score(old, ctx(List.of(), List.of(), null, "ANY", Set.of()), 0.0);
        assertThat(f.recency()).isCloseTo(0.5, within(0.05));
    }

    @Test
    void excludedCompanyDetected() {
        MatchContext ctx = ctx(List.of(), List.of(), null, "ANY", Set.of("evilcorp"));
        assertThat(scorer.isExcluded(job("Dev", "EvilCorp GmbH", "Berlin", ""), ctx)).isTrue();
        assertThat(scorer.isExcluded(job("Dev", "Nice GmbH", "Berlin", ""), ctx)).isFalse();
    }

    @Test
    void semanticIsClampedAndPassedThrough() {
        Features f = scorer.score(job("Dev", "Acme", "Berlin", ""),
                ctx(List.of(), List.of(), null, "ANY", Set.of()), 1.5);
        assertThat(f.semantic()).isEqualTo(1.0);
    }
}
