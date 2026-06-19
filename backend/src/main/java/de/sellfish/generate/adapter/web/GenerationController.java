package de.sellfish.generate.adapter.web;

import de.sellfish.common.security.CurrentUser;
import de.sellfish.generate.*;
import de.sellfish.generate.GenerationDtos.GenerateRequest;
import de.sellfish.generate.GenerationDtos.GeneratedResponse;
import de.sellfish.generate.GenerationDtos.UpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/generate")
public class GenerationController {

    private final GenerationService service;
    private final InterviewPrepService interviewPrep;

    public GenerationController(GenerationService service, InterviewPrepService interviewPrep) {
        this.service = service;
        this.interviewPrep = interviewPrep;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GeneratedResponse generate(@Valid @RequestBody GenerateRequest req) {
        return GeneratedResponse.from(service.generate(CurrentUser.id(), req.jobMatchId(), req.type()));
    }

    @GetMapping
    public List<GeneratedResponse> list(@RequestParam(required = false) UUID jobMatchId) {
        var docs = jobMatchId == null
                ? service.list(CurrentUser.id())
                : service.listForMatch(CurrentUser.id(), jobMatchId);
        return docs.stream().map(GeneratedResponse::from).toList();
    }

    @GetMapping("/{id}")
    public GeneratedResponse get(@PathVariable UUID id) {
        return GeneratedResponse.from(service.get(CurrentUser.id(), id));
    }

    @PutMapping("/{id}")
    public GeneratedResponse update(@PathVariable UUID id, @RequestBody UpdateRequest req) {
        return GeneratedResponse.from(service.updateContent(CurrentUser.id(), id, req.content()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(CurrentUser.id(), id);
    }

    // ── Interview Prep & Company Research ──

    public record InterviewQuestionsResponse(String questions) {}

    @PostMapping("/interview-questions/{matchId}")
    public InterviewQuestionsResponse interviewQuestions(@PathVariable UUID matchId) {
        return new InterviewQuestionsResponse(interviewPrep.generateQuestions(CurrentUser.id(), matchId));
    }

    public record CompanyResearchResponse(String profile) {}

    @PostMapping("/company-research/{matchId}")
    public CompanyResearchResponse companyResearch(@PathVariable UUID matchId) {
        return new CompanyResearchResponse(interviewPrep.generateCompanyResearch(CurrentUser.id(), matchId));
    }
}
