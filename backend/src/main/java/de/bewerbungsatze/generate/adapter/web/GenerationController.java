package de.bewerbungsatze.generate.adapter.web;
import de.bewerbungsatze.generate.*;

import de.bewerbungsatze.common.security.CurrentUser;
import de.bewerbungsatze.generate.GenerationDtos.GenerateRequest;
import de.bewerbungsatze.generate.GenerationDtos.GeneratedResponse;
import de.bewerbungsatze.generate.GenerationDtos.UpdateRequest;
import jakarta.validation.Valid;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/generate")
public class GenerationController {

    private final GenerationService service;

    public GenerationController(GenerationService service) {
        this.service = service;
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
}
