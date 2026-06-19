package de.sellfish.cv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.sellfish.ai.LlmService;
import de.sellfish.ai.model.ChatRequest;
import de.sellfish.ai.model.ChatResult;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CvParsingServiceTest {

    @Mock
    LlmService llmService;

    @Mock
    CvStructuredRepository cvRepository;

    @Mock
    ProjectRepository projectRepository;

    CvParsingService service;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        service = new CvParsingService(llmService, new ObjectMapper(), cvRepository, projectRepository);
    }

    private void mockChat(String json) {
        when(llmService.chat(any(UUID.class), any(ChatRequest.class)))
                .thenReturn(new ChatResult(json, "model", null, null));
    }

    @Test
    void parseCvPersistsStructuredFields() {
        UUID userId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        mockChat(
                """
                {"experience":[{"title":"Dev","company":"Acme","period":"2020-2024","description":""}],"education":[],"skills":["java","spring"],"languages":[],"certifications":["aws"]}""");
        CvStructured cv = new CvStructured(userId, docId);
        when(cvRepository.findByUserId(userId)).thenReturn(Optional.of(cv));
        when(cvRepository.save(cv)).thenReturn(cv);

        CvStructured result = service.parseCv(userId, docId, "raw cv text");

        assertThat(result.getSkills()).contains("java");
        assertThat(result.getExperience()).contains("Dev");
        assertThat(result.getCertifications()).contains("aws");
    }

    @Test
    void parseCvThrowsOnInvalidJson() {
        UUID userId = UUID.randomUUID();
        mockChat("not json at all");
        when(cvRepository.findByUserId(userId)).thenReturn(Optional.of(new CvStructured(userId, UUID.randomUUID())));
        assertThatThrownBy(() -> service.parseCv(userId, UUID.randomUUID(), "text"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void parseProjectsSkipsEmptyTitles() {
        UUID userId = UUID.randomUUID();
        mockChat(
                """
                [{"title":"Project A","role":"Lead","period":"","tech":["java"],"description":""},{"title":"","role":"","period":"","tech":[],"description":""}]""");
        doNothing().when(projectRepository).deleteByUserId(userId);
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        var projects = service.parseProjects(userId, "raw text");

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        assertThat(projects).hasSize(1);
        assertThat(captor.getValue().getTitle()).isEqualTo("Project A");
    }

    @Test
    void parseProjectsThrowsWhenNotArray() {
        UUID userId = UUID.randomUUID();
        mockChat("""
                {"not":"an array"}""");
        doNothing().when(projectRepository).deleteByUserId(userId);
        assertThatThrownBy(() -> service.parseProjects(userId, "text")).isInstanceOf(IllegalStateException.class);
    }
}
