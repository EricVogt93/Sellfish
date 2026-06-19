package de.sellfish.agent.adapter.web;

import de.sellfish.common.security.CurrentUser;
import de.sellfish.generate.GenerationDtos.GeneratedResponse;
import de.sellfish.generate.GenerationService;
import de.sellfish.generate.GenerationType;
import de.sellfish.jobs.JobSearchService;
import de.sellfish.jobs.SearchRun;
import de.sellfish.matching.MatchDtos.MatchResponse;
import de.sellfish.matching.MatchService;
import de.sellfish.matching.MatchStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Tool-Schnittstelle, über die ein externer AI-Agent die Plattform fernsteuern kann
 * (Suche auslösen, Matches lesen, Dokumente generieren). Authentifizierung wie üblich per JWT.
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final JobSearchService jobSearchService;
    private final MatchService matchService;
    private final GenerationService generationService;

    public AgentController(JobSearchService jobSearchService,
                           MatchService matchService,
                           GenerationService generationService) {
        this.jobSearchService = jobSearchService;
        this.matchService = matchService;
        this.generationService = generationService;
    }

    public record Tool(String name, String description, String method, String path, String input) {
    }

    public record SearchResult(UUID runId, String status, List<MatchResponse> topMatches) {
    }

    public record GenerateRequest(UUID jobMatchId, GenerationType type) {
    }

    @GetMapping("/tools")
    public List<Tool> tools() {
        return List.of(
                new Tool("search_jobs",
                        "Löst einen Search run aus und liefert die besten Matches.",
                        "POST", "/api/agent/search", "{}"),
                new Tool("list_matches",
                        "Listet die aktuellen Job-Matches des Nutzers (optional nach Status).",
                        "GET", "/api/agent/matches?status=&limit=", "{}"),
                new Tool("generate_document",
                        "Generiert ein Bewerbungsdokument für ein Match.",
                        "POST", "/api/agent/generate",
                        "{\"jobMatchId\":\"<uuid>\",\"type\":\"COVER_LETTER|MOTIVATION|TAILORED_CV|APPLICATION_TEXT\"}"));
    }

    @PostMapping("/search")
    public SearchResult search() {
        UUID userId = CurrentUser.id();
        SearchRun run = jobSearchService.runForUser(userId);
        List<MatchResponse> top = matchService.list(userId, null, PageRequest.of(0, 10)).getContent();
        return new SearchResult(run.getId(), run.getStatus(), top);
    }

    @GetMapping("/matches")
    public List<MatchResponse> matches(@RequestParam(required = false) MatchStatus status,
                                       @RequestParam(defaultValue = "20") int limit) {
        return matchService.list(CurrentUser.id(), status, PageRequest.of(0, Math.min(limit, 100)))
                .getContent();
    }

    @PostMapping("/generate")
    public GeneratedResponse generate(@RequestBody GenerateRequest req) {
        return GeneratedResponse.from(
                generationService.generate(CurrentUser.id(), req.jobMatchId(), req.type()));
    }
}
