package de.sellfish.reports;

import de.sellfish.generate.SalaryInsightsService;
import de.sellfish.tenant.OrgFilter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService service;
    private final SalaryInsightsService salaryService;

    public ReportController(ReportService service, SalaryInsightsService salaryService) {
        this.service = service;
        this.salaryService = salaryService;
    }

    @GetMapping("/summary")
    public ReportService.Summary summary(@RequestParam(defaultValue = "30") int days) {
        return service.summary(currentOrg(), days);
    }

    @GetMapping("/daily-matches")
    public List<ReportService.DailyBucket> dailyMatches(@RequestParam(defaultValue = "30") int days) {
        return service.dailyMatches(currentOrg(), days);
    }

    @GetMapping("/daily-generations")
    public List<ReportService.DailyBucket> dailyGenerations(@RequestParam(defaultValue = "30") int days) {
        return service.dailyGenerations(currentOrg(), days);
    }

    @GetMapping("/status-distribution")
    public Map<String, Long> statusDistribution() {
        return service.statusDistribution(currentOrg());
    }

    @GetMapping("/team")
    public List<ReportService.MemberStats> team() {
        UUID orgId = currentOrg();
        if (orgId == null) return List.of();
        return service.teamStats(orgId);
    }

    // ── Salary Insights ──

    @GetMapping("/salary-stats")
    public SalaryInsightsService.SalaryStats salaryStats(
            @RequestParam(required = false) String title, @RequestParam(required = false) String location) {
        return salaryService.salaryStats(title, location);
    }

    @GetMapping("/salary-by-title")
    public List<SalaryInsightsService.SalaryByTitle> salaryByTitle(@RequestParam(defaultValue = "20") int limit) {
        return salaryService.topTitles(limit);
    }

    private UUID currentOrg() {
        return OrgFilter.getOrgFromRequest();
    }
}
