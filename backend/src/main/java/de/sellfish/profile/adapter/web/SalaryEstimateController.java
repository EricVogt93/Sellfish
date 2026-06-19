package de.sellfish.profile.adapter.web;

import de.sellfish.profile.SalaryEstimateService;
import de.sellfish.profile.SalaryEstimateService.SalaryEstimate;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class SalaryEstimateController {

    private final SalaryEstimateService salaryEstimateService;

    public SalaryEstimateController(SalaryEstimateService salaryEstimateService) {
        this.salaryEstimateService = salaryEstimateService;
    }

    @GetMapping("/salary-estimate")
    public SalaryEstimate estimate() {
        UUID userId = de.sellfish.common.security.CurrentUser.id();
        return salaryEstimateService.estimate(userId);
    }
}
