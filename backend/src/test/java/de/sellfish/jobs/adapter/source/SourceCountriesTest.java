package de.sellfish.jobs.adapter.source;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class SourceCountriesTest {

    @Test
    void bundesagenturIsGermany() {
        assertThat(SourceCountries.countriesFor("BA")).containsExactly("DE");
    }

    @Test
    void adzunaCoversMultipleCountries() {
        Set<String> c = SourceCountries.countriesFor("ADZUNA");
        assertThat(c).contains("GB", "US", "DE");
    }

    @Test
    void remotiveIsRemote() {
        assertThat(SourceCountries.isRemote("REMOTIVE")).isTrue();
        assertThat(SourceCountries.isRemote("BA")).isFalse();
    }

    @Test
    void unknownCodeDefaultsToRemote() {
        assertThat(SourceCountries.countriesFor("DOESNOTEXIST")).containsExactly("REMOTE");
        assertThat(SourceCountries.isRemote("DOESNOTEXIST")).isTrue();
    }

    @Test
    void allGroupsContainsGermanyAndRemote() {
        var groups = SourceCountries.allGroups();
        assertThat(groups).anyMatch(g -> g.code().equals("DE"));
        assertThat(groups).anyMatch(g -> g.code().equals("REMOTE") && g.remote());
    }

    @Test
    void availableCountriesStartsWithDe() {
        assertThat(SourceCountries.availableCountries()).contains("DE", "AT", "REMOTE");
    }
}
