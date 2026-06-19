package de.sellfish.users;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void passwordUserHasEmailAndPasswordHash() {
        User user = new User("eric@x.com", "secret-hash");
        assertThat(user.getEmail()).isEqualTo("eric@x.com");
        assertThat(user.getPasswordHash()).isEqualTo("secret-hash");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void ssoUserCarriesOidcFields() {
        User user = User.sso("eric@x.com", "sub-123", "authentik");
        assertThat(user.getEmail()).isEqualTo("eric@x.com");
        assertThat(user.getOidcSubject()).isEqualTo("sub-123");
        assertThat(user.getOidcProvider()).isEqualTo("authentik");
        assertThat(user.getPasswordHash()).isEmpty();
    }

    @Test
    void setCurrentOrgIdStoresValue() {
        User user = new User("a@b.com", "h");
        java.util.UUID orgId = java.util.UUID.randomUUID();
        user.setCurrentOrgId(orgId);
        assertThat(user.getCurrentOrgId()).isEqualTo(orgId);
    }
}
