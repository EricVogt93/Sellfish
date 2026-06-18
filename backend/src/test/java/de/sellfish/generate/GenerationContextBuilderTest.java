package de.sellfish.generate;

import de.sellfish.cv.CvStructured;
import de.sellfish.cv.CvStructuredRepository;
import de.sellfish.cv.Project;
import de.sellfish.cv.ProjectRepository;
import de.sellfish.jobs.Job;
import de.sellfish.profile.ProfileRepository;
import de.sellfish.profile.UserProfile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GenerationContextBuilderTest {

    private final ProfileRepository profileRepository = mock(ProfileRepository.class);
    private final CvStructuredRepository cvRepository = mock(CvStructuredRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);

    private final GenerationContextBuilder builder =
            new GenerationContextBuilder(profileRepository, cvRepository, projectRepository);

    @Test
    void includesJobProfileCvAndProjects() {
        UUID userId = UUID.randomUUID();
        Job job = new Job("BA", "fp", "Senior Java Entwickler");
        job.setCompany("Acme GmbH");
        job.setDescription("Spring, Kafka, Cloud");

        UserProfile profile = new UserProfile(userId);
        profile.setHeadline("Erfahrener Backend-Entwickler");
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        CvStructured cv = new CvStructured(userId, null);
        cv.setSkills("[\"Java\",\"Spring\"]");
        when(cvRepository.findByUserId(userId)).thenReturn(Optional.of(cv));

        Project project = new Project(userId, "Zahlungsplattform");
        project.setRole("Lead");
        project.setTech(new String[]{"Java", "Kafka"});
        when(projectRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(project));

        String context = builder.build(userId, job);

        assertThat(context)
                .contains("Senior Java Entwickler")
                .contains("Acme GmbH")
                .contains("Erfahrener Backend-Entwickler")
                .contains("Java")
                .contains("Zahlungsplattform")
                .contains("Lead");
    }

    @Test
    void omitsEmptyCvSections() {
        UUID userId = UUID.randomUUID();
        when(profileRepository.findByUserId(any())).thenReturn(Optional.empty());
        when(cvRepository.findByUserId(any())).thenReturn(Optional.empty());
        when(projectRepository.findByUserIdOrderByCreatedAtDesc(any())).thenReturn(List.of());

        String context = builder.build(userId, new Job("BA", "fp", "Tester"));
        assertThat(context).contains("Tester").doesNotContain("Fähigkeiten");
    }
}
