package de.bewerbungsatze.users.adapter.web;
import de.bewerbungsatze.users.*;

import de.bewerbungsatze.common.error.ApiException;
import de.bewerbungsatze.common.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/me")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public record MeResponse(UUID id, String email, String role, String locale) {
    }

    @GetMapping
    public MeResponse me() {
        User user = userRepository.findById(CurrentUser.id())
                .orElseThrow(() -> ApiException.notFound("Nutzer nicht gefunden"));
        return new MeResponse(user.getId(), user.getEmail(), user.getRole().name(), user.getLocale());
    }
}
