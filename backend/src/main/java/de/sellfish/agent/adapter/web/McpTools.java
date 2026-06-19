package de.sellfish.agent.adapter.web;

import de.sellfish.agent.mcp.McpTool;
import de.sellfish.agent.mcp.McpToolParam;
import de.sellfish.common.security.CurrentUser;
import de.sellfish.generate.GenerationDtos.GeneratedResponse;
import de.sellfish.generate.GenerationService;
import de.sellfish.generate.GenerationType;
import de.sellfish.jobs.JobSearchService;
import de.sellfish.jobs.SearchRun;
import de.sellfish.matching.MatchDtos.MatchResponse;
import de.sellfish.matching.MatchService;
import de.sellfish.matching.MatchStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * MCP Tools — via Spring AI MCP-SDK automatisch als JSON-RPC-Tools exponiert.
 * Externe AI-Agenten (Claude Desktop, Continue, Cursor, …) können darüber
 * Suchläufe starten, Matches lesen und Bewerbungsdokumente generieren.
 */
@Component
public class McpTools {

    private final JobSearchService jobSearchService;
    private final MatchService matchService;
    private final GenerationService generationService;

    public McpTools(JobSearchService jobSearchService, MatchService matchService, GenerationService generationService) {
        this.jobSearchService = jobSearchService;
        this.matchService = matchService;
        this.generationService = generationService;
    }

    @McpTool(description = "Löst einen Search run über alle Job sourcen aus und liefert die besten Matches zurück")
    public McpSearchResult searchJobs() {
        UUID userId = CurrentUser.id();
        SearchRun run = jobSearchService.runForUser(userId);
        List<MatchResponse> top =
                matchService.list(userId, null, PageRequest.of(0, 10)).getContent();
        return new McpSearchResult(run.getId(), run.getStatus(), top);
    }

    @McpTool(description = "Listet die aktuellen Job-Matches des Nutzers, optional gefiltert nach Status")
    public List<McpMatch> listMatches(
            @McpToolParam(
                            description =
                                    "Match-Status zum Filtern (NEW, SEEN, SAVED, DISMISSED, APPLIED, INTERVIEW, OFFER, REJECTED)")
                    String status,
            @McpToolParam(description = "Maximale Anzahl Matches (1-100, default 20)") int limit) {
        UUID userId = CurrentUser.id();
        MatchStatus ms = status != null && !status.isBlank() ? MatchStatus.valueOf(status.toUpperCase()) : null;
        int clamped = limit > 0 ? Math.min(limit, 100) : 20;
        return matchService.list(userId, ms, PageRequest.of(0, clamped)).getContent().stream()
                .map(m -> new McpMatch(
                        m.matchId(),
                        m.title(),
                        m.company(),
                        m.location(),
                        m.url(),
                        m.score(),
                        m.status().name(),
                        m.source()))
                .toList();
    }

    @McpTool(description = "Generiert ein Bewerbungsdokument für ein Job-Match (Anschreiben, Motivation, CV, Kurztext)")
    public String generateDocument(
            @McpToolParam(description = "UUID des Job-Matches") String jobMatchId,
            @McpToolParam(description = "Dokument-Typ: TAILORED_CV, COVER_LETTER, MOTIVATION oder APPLICATION_TEXT")
                    String type) {
        UUID matchId = UUID.fromString(jobMatchId);
        GenerationType genType = GenerationType.valueOf(type.toUpperCase());
        GeneratedResponse result =
                GeneratedResponse.from(generationService.generate(CurrentUser.id(), matchId, genType));
        return result.content();
    }

    // ── Tool-Return-Types (für saubere JSON-Serialisierung) ──

    public record McpSearchResult(UUID runId, String status, List<MatchResponse> topMatches) {}

    public record McpMatch(
            UUID matchId,
            String title,
            String company,
            String location,
            String url,
            double score,
            String status,
            String source) {}
}
