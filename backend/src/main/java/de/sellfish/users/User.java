package de.sellfish.users;

import de.sellfish.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(nullable = false)
    private String locale = "de-DE";

    @Column(name = "current_org_id")
    private UUID currentOrgId;

    @Column(name = "oidc_subject")
    private String oidcSubject;

    @Column(name = "oidc_provider")
    private String oidcProvider;

    protected User() {
    }

    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public static User sso(String email, String oidcSubject, String oidcProvider) {
        User user = new User(email, "");
        user.oidcSubject = oidcSubject;
        user.oidcProvider = oidcProvider;
        return user;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public UUID getCurrentOrgId() {
        return currentOrgId;
    }

    public void setCurrentOrgId(UUID currentOrgId) {
        this.currentOrgId = currentOrgId;
    }

    public String getOidcSubject() {
        return oidcSubject;
    }

    public String getOidcProvider() {
        return oidcProvider;
    }
}
