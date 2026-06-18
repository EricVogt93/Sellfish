package de.bewerbungsatze.beta;

import de.bewerbungsatze.common.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/beta")
public class AutoSetupController {

    private final AutoSetupService service;

    public AutoSetupController(AutoSetupService service) {
        this.service = service;
    }

    public record SetupResponse(String status, String message) {}

    @PostMapping("/auto-setup")
    @ResponseStatus(HttpStatus.CREATED)
    public SetupResponse autoSetup() {
        var result = service.run(CurrentUser.id());
        return new SetupResponse(result.status(), result.message());
    }
}
