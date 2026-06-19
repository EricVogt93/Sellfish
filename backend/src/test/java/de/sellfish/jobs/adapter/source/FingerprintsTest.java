package de.sellfish.jobs.adapter.source;

import static org.assertj.core.api.Assertions.assertThat;

import de.sellfish.jobs.port.RawJob;
import org.junit.jupiter.api.Test;

class FingerprintsTest {

    private RawJob job(String source, String ref, String title, String company, String location) {
        return new RawJob(source, ref, title, company, location, null, null, null, null, null, null);
    }

    @Test
    void usesExternalRefWhenPresent() {
        String a = Fingerprints.of(job("BA", "123", "Dev", "Acme", "Berlin"));
        String b = Fingerprints.of(job("BA", "123", "Anderer Titel", "Andere Firma", "München"));
        assertThat(a).isEqualTo(b);
    }

    @Test
    void differentRefsDiffer() {
        assertThat(Fingerprints.of(job("BA", "1", "x", "y", "z")))
                .isNotEqualTo(Fingerprints.of(job("BA", "2", "x", "y", "z")));
    }

    @Test
    void fallsBackToContentAndNormalizes() {
        String a = Fingerprints.of(job("X", null, "Senior  Java   Dev", "ACME", "Berlin"));
        String b = Fingerprints.of(job("X", null, "senior java dev", "acme", "berlin"));
        assertThat(a).isEqualTo(b);
    }

    @Test
    void producesSha256Hex() {
        assertThat(Fingerprints.of(job("X", "ref", "t", "c", "l"))).hasSize(64).matches("[0-9a-f]+");
    }
}
