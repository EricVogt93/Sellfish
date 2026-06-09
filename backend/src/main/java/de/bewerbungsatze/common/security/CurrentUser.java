package de.bewerbungsatze.common.security;

import de.bewerbungsatze.auth.AppUserDetails;
import de.bewerbungsatze.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Liest den aktuell authentifizierten Nutzer aus dem SecurityContext.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static AppUserDetails details() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserDetails details) {
            return details;
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "Nicht authentifiziert");
    }

    public static UUID id() {
        return details().getId();
    }
}
