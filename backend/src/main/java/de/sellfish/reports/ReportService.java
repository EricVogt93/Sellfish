package de.sellfish.reports;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private final EntityManager em;

    public ReportService(EntityManager em) {
        this.em = em;
    }

    @Transactional(readOnly = true)
    public Summary summary(UUID orgId, int days) {
        if (days < 1) days = 30;
        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        String org = orgId != null ? " AND m.org_id = :orgId" : "";
        String jobOrg = orgId != null ? " AND j.org_id = :orgId" : "";

        String sql = "SELECT"
                + " (SELECT count(*) FROM job_matches m WHERE m.created_at >= :since" + org + "),"
                + " (SELECT count(*) FROM job_matches m WHERE m.status = 'APPLIED' AND m.updated_at >= :since" + org
                + "),"
                + " (SELECT count(*) FROM job_matches m WHERE m.status IN ('INTERVIEW','OFFER') AND m.updated_at >= :since"
                + org + "),"
                + " (SELECT count(*) FROM generated_documents g WHERE g.created_at >= :since" + org + "),"
                + " (SELECT count(*) FROM jobs j WHERE j.created_at >= :since" + jobOrg + "),"
                + " (SELECT count(*) FROM users)";

        Query q = em.createNativeQuery(sql).setParameter("since", since);
        if (orgId != null) q.setParameter("orgId", orgId);

        Object[] row = (Object[]) q.getSingleResult();
        return new Summary(
                ((Number) row[0]).longValue(),
                ((Number) row[1]).longValue(),
                ((Number) row[2]).longValue(),
                ((Number) row[3]).longValue(),
                ((Number) row[4]).longValue(),
                ((Number) row[5]).longValue());
    }

    @Transactional(readOnly = true)
    public List<DailyBucket> dailyMatches(UUID orgId, int days) {
        if (days < 1) days = 30;
        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        String org = orgId != null ? "AND org_id = :orgId" : "";

        String sql = "SELECT to_char(date_trunc('day', created_at), 'YYYY-MM-DD') AS day, count(*)"
                + " FROM job_matches"
                + " WHERE created_at >= :since " + org
                + " GROUP BY day ORDER BY day";

        Query q = em.createNativeQuery(sql).setParameter("since", since);
        if (orgId != null) q.setParameter("orgId", orgId);
        return mapBuckets(q.getResultList());
    }

    @Transactional(readOnly = true)
    public List<DailyBucket> dailyGenerations(UUID orgId, int days) {
        if (days < 1) days = 30;
        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        String org = orgId != null ? "AND org_id = :orgId" : "";

        String sql = "SELECT to_char(date_trunc('day', created_at), 'YYYY-MM-DD') AS day, count(*)"
                + " FROM generated_documents"
                + " WHERE created_at >= :since " + org
                + " GROUP BY day ORDER BY day";

        Query q = em.createNativeQuery(sql).setParameter("since", since);
        if (orgId != null) q.setParameter("orgId", orgId);
        return mapBuckets(q.getResultList());
    }

    @Transactional(readOnly = true)
    public Map<String, Long> statusDistribution(UUID orgId) {
        String org = orgId != null ? "AND org_id = :orgId" : "";

        String sql = "SELECT status, count(*)"
                + " FROM job_matches"
                + " WHERE 1=1 " + org
                + " GROUP BY status ORDER BY count(*) DESC";

        Query q = em.createNativeQuery(sql);
        if (orgId != null) q.setParameter("orgId", orgId);

        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : (List<Object[]>) q.getResultList()) {
            map.put((String) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }

    @Transactional(readOnly = true)
    public List<MemberStats> teamStats(UUID orgId) {
        if (orgId == null) return List.of();

        String sql = "SELECT m.user_id, u.email,"
                + " count(*) FILTER (WHERE jm.status = 'APPLIED'),"
                + " count(*) FILTER (WHERE jm.status IN ('INTERVIEW','OFFER')),"
                + " count(*) FILTER (WHERE g.id IS NOT NULL)"
                + " FROM org_member m"
                + " JOIN users u ON u.id = m.user_id"
                + " LEFT JOIN job_matches jm ON jm.user_id = m.user_id AND jm.org_id = :orgId"
                + " LEFT JOIN generated_documents g ON g.user_id = m.user_id AND g.org_id = :orgId"
                + " WHERE m.org_id = :orgId"
                + " GROUP BY m.user_id, u.email"
                + " ORDER BY count(*) FILTER (WHERE jm.status = 'APPLIED') DESC";

        Query q = em.createNativeQuery(sql).setParameter("orgId", orgId);

        List<MemberStats> list = new ArrayList<>();
        for (Object[] row : (List<Object[]>) q.getResultList()) {
            list.add(new MemberStats(
                    ((UUID) row[0]).toString(),
                    (String) row[1],
                    ((Number) row[2]).longValue(),
                    ((Number) row[3]).longValue(),
                    ((Number) row[4]).longValue()));
        }
        return list;
    }

    private List<DailyBucket> mapBuckets(List<?> rows) {
        List<DailyBucket> buckets = new ArrayList<>();
        for (Object[] row : (List<Object[]>) rows) {
            buckets.add(new DailyBucket((String) row[0], ((Number) row[1]).longValue()));
        }
        return buckets;
    }

    public record Summary(
            long totalMatches, long applied, long interviews, long generated, long jobsScanned, long totalUsers) {}

    public record DailyBucket(String day, long count) {}

    public record MemberStats(String userId, String email, long applied, long interviews, long generated) {}
}
