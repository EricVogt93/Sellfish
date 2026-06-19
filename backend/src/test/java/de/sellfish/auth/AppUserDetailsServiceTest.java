package de.sellfish.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import de.sellfish.users.User;
import de.sellfish.users.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    AppUserDetailsService service;

    @Test
    void loadsUserByEmail() {
        User user = new User("eric@x.com", "hash");
        when(userRepository.findByEmailIgnoreCase("eric@x.com")).thenReturn(Optional.of(user));
        var details = service.loadUserByUsername("eric@x.com");
        assertThat(details.getUsername()).isEqualTo("eric@x.com");
    }

    @Test
    void throwsWhenUserNotFound() {
        when(userRepository.findByEmailIgnoreCase("ghost@x.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.loadUserByUsername("ghost@x.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
