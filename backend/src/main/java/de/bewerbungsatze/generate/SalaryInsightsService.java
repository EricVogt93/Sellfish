package de.bewerbungsatze.generate;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalaryInsightsService {

    private final EntityManager em;
    private final ObjectMapper mapper = new ObjectMapper();

    public SalaryInsightsService(EntityManager em) {
        this.em = em;
    }

    @Transactional(readOnly = true)
    public SalaryStats salaryStats(String title, String location) {
        StringBuilder sql = new StringBuilder(
                "SELECT count(*), percentile_disc(0.25) WITHIN GROUP (ORDER BY s.num) FILTER (WHERE s.num > 0),"
                + " percentile_disc(0.50) WITHIN GROUP (ORDER BY s.num) FILTER (WHERE s.num > 0),"
                + " percentile_disc(0.75) WITHIN GROUP (ORDER BY s.num) FILTER (WHERE s.num > 0),"
                + " avg(s.num) FILTER (WHERE s.num > 0)"
                + " FROM jobs j, LATERAL (SELECT (regexp_matches(COALESCE(j.salary_raw,''), '(\\d[\\d.,]*)', 'g'))[1]::numeric AS num) s"
                + " WHERE j.salary_raw IS NOT NULL AND j.salary_raw != ''");

        if (title != null && !title.isBlank()) {
            sql.append(" AND j.title ILIKE :title");
        }
        if (location != null && !location.isBlank()) {
            sql.append(" AND j.location ILIKE :location");
        }

        Query q = em.createNativeQuery(sql.toString());
        if (title != null && !title.isBlank()) q.setParameter("title", "%" + title + "%");
        if (location != null && !location.isBlank()) q.setParameter("location", "%" + location + "%");

        Object[] row = (Object[]) q.getSingleResult();
        long count = ((Number) row[0]).longValue();
        Double p25 = row[1] != null ? ((Number) row[1]).doubleValue() : null;
        Double p50 = row[2] != null ? ((Number) row[2]).doubleValue() : null;
        Double p75 = row[3] != null ? ((Number) row[3]).doubleValue() : null;
        Double avg = row[4] != null ? ((Number) row[4]).doubleValue() : null;

        return new SalaryStats(count, round(p25), round(p50), round(p75), round(avg));
    }

    @Transactional(readOnly = true)
    public List<SalaryByTitle> topTitles(int limit) {
        String sql = """
                SELECT j.title, count(*) AS cnt, avg(s.num) FILTER (WHERE s.num > 0) AS avg_sal
                FROM jobs j, LATERAL (SELECT (regexp_matches(COALESCE(j.salary_raw,''), '(\\\\d[\\\\d.,]*)', 'g'))[1]::numeric AS num) s
                WHERE j.salary_raw IS NOT NULL AND j.salary_raw != ''
                GROUP BY j.title HAVING count(*) >= 3
                ORDER BY cnt DESC LIMIT :limit""";

        Query q = em.createNativeQuery(sql).setParameter("limit", Math.min(50, limit));
        List<SalaryByTitle> list = new ArrayList<>();
        for (Object[] row : (List<Object[]>) q.getResultList()) {
            list.add(new SalaryByTitle((String) row[0], ((Number) row[1]).longValue(), round(((Number) row[2]).doubleValue())));
        }
        return list;
    }

    private Double round(Double v) {
        return v == null ? null : Math.round(v * 100.0) / 100.0;
    }

    public record SalaryStats(long count, Double p25, Double p50, Double p75, Double avg) {}
    public record SalaryByTitle(String title, long count, Double avgSalary) {}
}
