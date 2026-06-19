package de.sellfish.account.adapter.web;

import de.sellfish.account.AccountService;
import de.sellfish.common.security.CurrentUser;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound-Adapter für DSGVO-Selbstbedienung: Datenexport und Account-Löschung.
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @GetMapping("/export")
    public Map<String, Object> export() {
        return service.export(CurrentUser.id());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount() {
        service.deleteAccount(CurrentUser.id());
    }
}
