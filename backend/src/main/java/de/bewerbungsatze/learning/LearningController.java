package de.bewerbungsatze.learning;

import de.bewerbungsatze.common.security.CurrentUser;
import de.bewerbungsatze.learning.SelfLearningService.RetrainResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/learning")
public class LearningController {

    private final SelfLearningService service;

    public LearningController(SelfLearningService service) {
        this.service = service;
    }

    @PostMapping("/retrain")
    public RetrainResult retrain() {
        return service.retrain(CurrentUser.id());
    }

    @GetMapping("/model")
    public Map<String, Double> currentModel() {
        return service.currentWeights(CurrentUser.id()).asMap();
    }
}
