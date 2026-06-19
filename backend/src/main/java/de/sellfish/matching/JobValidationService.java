package de.sellfish.matching;

import de.sellfish.ai.LlmService;
import de.sellfish.ai.model.ChatRequest;
import de.sellfish.ai.model.ChatResult;
import de.sellfish.common.text.Strings;
import de.sellfish.jobs.Job;
import de.sellfish.profile.UserPreferences;
import de.sellfish.profile.UserProfile;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * LLM-basierte Job-Validierung: Relevanz-Score (0–1), Qualitäts-Flag,
 * Skill-Extraktion. Läuft zusätzlich zum regelbasierten FeatureScorer
 * und fließt als eigenes Merkmal in die Match-Berechnung ein.
 */
@Service
public class JobValidationService {

    private static final Logger log = LoggerFactory.getLogger(JobValidationService.class);

    private final LlmService llmService;

    public JobValidationService(LlmService llmService) {
        this.llmService = llmService;
    }

    /**
     * LLM-basierter Relevanz-Score (0..1). Fragt das Modell, wie gut der Job
     * zum Profil passt, und erzwingt eine numerische Antwort.
     */
    public double relevanceScore(UUID userId, Job job, UserProfile profile, UserPreferences prefs) {
        String prompt = buildRelevancePrompt(job, profile, prefs);
        try {
            ChatResult result = llmService.chat(
                    userId, ChatRequest.of("You score job relevance 0..100. Answer with only a number.", prompt));
            String text = result.content() != null ? result.content().strip() : "";
            return parseScore(text);
        } catch (Exception e) {
            log.debug("LLM-Relevanz-Score failed: {}", e.getMessage());
            return 0.5;
        }
    }

    /**
     * Prüft, ob der Job vermutlich Spam/Fake/Multi-Level-Marketing ist.
     * {@code true} = legit, {@code false} = verdächtig.
     */
    public boolean isLegitimate(UUID userId, Job job) {
        String prompt = buildQualityPrompt(job);
        try {
            ChatResult result = llmService.chat(
                    userId, ChatRequest.of("You classify job postings. Answer only 'yes' or 'no'.", prompt));
            String text = result.content() != null ? result.content().strip().toLowerCase() : "";
            return text.contains("yes")
                    || text.contains("legit")
                    || text.contains("real")
                    || text.contains("legitimate");
        } catch (Exception e) {
            log.debug("isLegitimate check failed, defaulting to legitimate: {}", e.getMessage());
            return true;
        }
    }

    private String buildRelevancePrompt(Job job, UserProfile profile, UserPreferences prefs) {
        StringBuilder sb = new StringBuilder("You are a job matching assistant. ");
        sb.append("Rate on a scale from 0 to 100 how well this job fits the user's profile. ");
        sb.append("Reply with ONLY a single number (0-100).\n\n");

        sb.append("--- JOB ---\n");
        sb.append("Title: ").append(Strings.nz(job.getTitle())).append('\n');
        sb.append("Company: ").append(Strings.nz(job.getCompany())).append('\n');
        sb.append("Location: ").append(Strings.nz(job.getLocation())).append('\n');
        sb.append("Description: ")
                .append(Strings.truncate(Strings.nz(job.getDescription()), 1500))
                .append('\n');

        sb.append("\n--- USER PROFILE ---\n");
        if (profile != null) {
            sb.append("Headline: ").append(Strings.nz(profile.getHeadline())).append('\n');
            sb.append("Summary: ")
                    .append(Strings.truncate(Strings.nz(profile.getSummary()), 500))
                    .append('\n');
            sb.append("Location: ").append(Strings.nz(profile.getLocation())).append('\n');
        }
        if (prefs != null) {
            sb.append("Desired titles: ").append(arr(prefs.getDesiredTitles())).append('\n');
            sb.append("Keywords: ").append(arr(prefs.getKeywords())).append('\n');
        }
        sb.append("\nScore (0-100): ");
        return sb.toString();
    }

    private String buildQualityPrompt(Job job) {
        return "Is this a legitimate job posting? Reply ONLY 'yes' or 'no'.\n\n"
                + "Title: " + Strings.nz(job.getTitle()) + "\n"
                + "Company: " + Strings.nz(job.getCompany()) + "\n"
                + "Description: " + Strings.truncate(Strings.nz(job.getDescription()), 800) + "\n"
                + "Legitimate? (yes/no): ";
    }

    static double parseScore(String text) {
        text = text.replaceAll("[^0-9.]", " ").trim();
        for (String part : text.split("\\s+")) {
            try {
                double v = Double.parseDouble(part);
                if (v >= 0 && v <= 100) return Math.min(1.0, Math.max(0.0, v / 100.0));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0.5;
    }

    private String arr(String[] a) {
        return a == null ? "" : String.join(", ", a);
    }
}
