package de.bewerbungsatze.matching.adapter.web;
import de.bewerbungsatze.matching.*;

import de.bewerbungsatze.common.security.CurrentUser;
import de.bewerbungsatze.matching.MatchDtos.MatchResponse;
import de.bewerbungsatze.matching.MatchDtos.StatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping
    public Page<MatchResponse> list(@RequestParam(required = false) MatchStatus status,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return matchService.list(CurrentUser.id(), status, PageRequest.of(page, Math.min(size, 100)));
    }

    @PostMapping("/{id}/status")
    public MatchResponse updateStatus(@PathVariable UUID id, @RequestBody StatusRequest req) {
        return matchService.updateStatus(CurrentUser.id(), id, req.status());
    }
}
