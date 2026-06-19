package de.sellfish.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    AuditRepository repository;

    @InjectMocks
    AuditService service;

    @Test
    void recordPersistsEventWithDetails() {
        UUID user = UUID.randomUUID();
        service.record(user, AuditAction.LOGIN, "user", user.toString());

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(captor.capture());
        AuditEvent saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(user);
        assertThat(saved.getAction()).isEqualTo(AuditAction.LOGIN);
        assertThat(saved.getTargetId()).isEqualTo(user.toString());
    }

    @Test
    void recordWithDetailsMapSerializesToJson() {
        UUID user = UUID.randomUUID();
        service.record(user, null, AuditAction.SEARCH_RUN, "search", "abc", Map.of("count", 5));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getDetails()).contains("\"count\"");
    }

    @Test
    void recordWithoutRequestContextStillPersists() {
        // No servlet request context active -> org/ip null but event still saved
        service.record(UUID.randomUUID(), AuditAction.LOGOUT);
        verify(repository).save(any(AuditEvent.class));
    }
}
