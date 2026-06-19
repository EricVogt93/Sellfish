package de.sellfish.ai.adapter.web;

import de.sellfish.ai.*;
import de.sellfish.ai.LlmConfigDtos.ConfigRequest;
import de.sellfish.ai.LlmConfigDtos.ConfigResponse;
import de.sellfish.ai.LlmConfigDtos.TestResult;
import de.sellfish.common.security.CurrentUser;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/llm/configs")
public class LlmConfigController {

    private final LlmConfigService service;

    public LlmConfigController(LlmConfigService service) {
        this.service = service;
    }

    @GetMapping
    public List<ConfigResponse> list() {
        return service.list(CurrentUser.id()).stream().map(ConfigResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConfigResponse create(@Valid @RequestBody ConfigRequest req) {
        return ConfigResponse.from(service.create(CurrentUser.id(), req));
    }

    @PutMapping("/{id}")
    public ConfigResponse update(@PathVariable UUID id, @Valid @RequestBody ConfigRequest req) {
        return ConfigResponse.from(service.update(CurrentUser.id(), id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(CurrentUser.id(), id);
    }

    @PostMapping("/{id}/test")
    public TestResult test(@PathVariable UUID id) {
        return service.test(CurrentUser.id(), id);
    }
}
