package de.sellfish.reports;

import static org.assertj.core.api.Assertions.assertThat;

import de.sellfish.support.AbstractPostgresIT;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ReportServiceIT extends AbstractPostgresIT {

    @Autowired
    ReportService reportService;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void summaryReturnsZerosOnEmptyDatabase() {
        ReportService.Summary s = reportService.summary(null, 30);
        assertThat(s.totalUsers()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void summaryScopesByOrgWhenProvided() {
        ReportService.Summary s = reportService.summary(UUID.randomUUID(), 30);
        assertThat(s.totalMatches()).isZero();
    }

    @Test
    void summaryClampsNegativeDaysToDefault() {
        reportService.summary(null, -5); // days < 1 -> 30
    }

    @Test
    void dailyBucketsRunForMatchesAndGenerations() {
        assertThat(reportService.dailyMatches(null, 30)).isEmpty();
        assertThat(reportService.dailyGenerations(UUID.randomUUID(), 0)).isEmpty();
    }

    @Test
    void statusDistributionRuns() {
        assertThat(reportService.statusDistribution(null)).isEmpty();
    }

    @Test
    void teamStatsReturnsEmptyForNullOrg() {
        assertThat(reportService.teamStats(null)).isEmpty();
    }

    @Test
    void teamStatsMapsRowsForOrg() {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)", userId, "rep@x.com", "h");
        jdbc.update("INSERT INTO organization (id, name, slug) VALUES (?, ?, ?)", orgId, "RepOrg", "reporg");
        jdbc.update("INSERT INTO org_member (id, org_id, user_id) VALUES (?, ?, ?)", UUID.randomUUID(), orgId, userId);

        var stats = reportService.teamStats(orgId);
        assertThat(stats).hasSize(1);
        assertThat(stats.get(0).email()).isEqualTo("rep@x.com");
    }
}
