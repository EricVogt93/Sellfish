package de.sellfish.generate;

import de.sellfish.cv.CvStructured;
import de.sellfish.cv.CvStructuredRepository;
import de.sellfish.cv.Project;
import de.sellfish.cv.ProjectRepository;
import de.sellfish.jobs.Job;
import de.sellfish.profile.ProfileRepository;
import de.sellfish.profile.UserProfile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Baut den Eingabekontext (Bewerberdaten + Zielstelle) für die Generatoren zusammen.
 */
@Component
public class GenerationContextBuilder {

    private final ProfileRepository profileRepository;
    private final CvStructuredRepository cvRepository;
    private final ProjectRepository projectRepository;

    public GenerationContextBuilder(ProfileRepository profileRepository,
                                    CvStructuredRepository cvRepository,
                                    ProjectRepository projectRepository) {
        this.profileRepository = profileRepository;
        this.cvRepository = cvRepository;
        this.projectRepository = projectRepository;
    }

    public String build(UUID userId, Job job) {
        StringBuilder sb = new StringBuilder();

        sb.append("# Zielstelle\n");
        sb.append("Titel: ").append(job.getTitle()).append('\n');
        if (job.getCompany() != null) {
            sb.append("Unternehmen: ").append(job.getCompany()).append('\n');
        }
        if (job.getLocation() != null) {
            sb.append("Ort: ").append(job.getLocation()).append('\n');
        }
        if (job.getDescription() != null) {
            sb.append("Beschreibung:\n").append(job.getDescription()).append('\n');
        }

        sb.append("\n# Bewerberprofil\n");
        UserProfile profile = profileRepository.findByUserId(userId).orElse(null);
        if (profile != null) {
            appendIf(sb, "Kurzprofil", profile.getHeadline());
            appendIf(sb, "Zusammenfassung", profile.getSummary());
            appendIf(sb, "Standort", profile.getLocation());
        }

        CvStructured cv = cvRepository.findByUserId(userId).orElse(null);
        if (cv != null) {
            appendIf(sb, "Berufserfahrung (JSON)", cv.getExperience());
            appendIf(sb, "Ausbildung (JSON)", cv.getEducation());
            appendIf(sb, "Fähigkeiten (JSON)", cv.getSkills());
            appendIf(sb, "Sprachen (JSON)", cv.getLanguages());
            appendIf(sb, "Zertifikate (JSON)", cv.getCertifications());
        }

        List<Project> projects = projectRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (!projects.isEmpty()) {
            sb.append("\n## Projekte\n");
            for (Project p : projects) {
                sb.append("- ").append(p.getTitle());
                if (p.getRole() != null) {
                    sb.append(" (").append(p.getRole()).append(')');
                }
                if (p.getTech() != null && p.getTech().length > 0) {
                    sb.append(" – ").append(String.join(", ", p.getTech()));
                }
                if (p.getDescription() != null) {
                    sb.append(": ").append(p.getDescription());
                }
                sb.append('\n');
            }
        }

        return sb.toString();
    }

    private void appendIf(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank() && !value.equals("[]") && !value.equals("{}")) {
            sb.append(label).append(": ").append(value).append('\n');
        }
    }
}
