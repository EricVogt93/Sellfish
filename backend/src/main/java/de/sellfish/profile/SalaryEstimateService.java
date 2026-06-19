package de.sellfish.profile;

import de.sellfish.ai.LlmService;
import de.sellfish.ai.model.ChatRequest;
import de.sellfish.ai.model.ChatResult;
import de.sellfish.common.json.JsonExtractor;
import de.sellfish.common.text.Strings;
import de.sellfish.cv.CvStructured;
import de.sellfish.cv.CvStructuredRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Estimates a user's market salary range using LLM analysis of their profile,
 * CV data (skills, experience, education) and location. Combines the LLM estimate
 * with structured data to produce a realistic salary band.
 */
@Service
public class SalaryEstimateService {

    private static final Logger log = LoggerFactory.getLogger(SalaryEstimateService.class);

    private final ProfileRepository profileRepository;
    private final CvStructuredRepository cvRepository;
    private final LlmService llmService;

    public SalaryEstimateService(
            ProfileRepository profileRepository, CvStructuredRepository cvRepository, LlmService llmService) {
        this.profileRepository = profileRepository;
        this.cvRepository = cvRepository;
        this.llmService = llmService;
    }

    public SalaryEstimate estimate(UUID userId) {
        UserProfile profile = profileRepository.findByUserId(userId).orElse(null);
        CvStructured cv = cvRepository.findByUserId(userId).orElse(null);

        if (profile == null) {
            return SalaryEstimate.empty("Complete your profile for a salary estimate.");
        }

        String context = buildContext(profile, cv);
        String prompt =
                """
                You are a salary market analyst. Based on the candidate's profile below,
                estimate their realistic gross annual salary range in EUR (European market)
                or USD (US market). Consider: role seniority, years of experience, tech stack,
                location, education, and current market conditions.

                Return ONLY valid JSON:
                {
                  "currency": "EUR" | "USD" | "GBP",
                  "low": 75000,
                  "median": 92000,
                  "high": 115000,
                  "confidence": "high" | "medium" | "low",
                  "factors": ["10+ years backend", "Kafka/Kubernetes premium skills", "Berlin market rate"],
                  "marketNote": "Senior backend engineers in Berlin with Kafka experience typically earn €85k–€110k."
                }

                Profile:
                """
                        + context;

        try {
            ChatResult result = llmService.chat(
                    userId, ChatRequest.of("You estimate realistic salaries. Reply ONLY with valid JSON.", prompt));
            return parseEstimate(JsonExtractor.extract(result.content()));
        } catch (Exception e) {
            log.warn("Salary estimate failed for {}: {}", userId, e.getMessage());
            return SalaryEstimate.empty("AI salary estimate temporarily unavailable.");
        }
    }

    private String buildContext(UserProfile profile, CvStructured cv) {
        StringBuilder sb = new StringBuilder();
        sb.append("Headline: ").append(Strings.nz(profile.getHeadline())).append('\n');
        sb.append("Summary: ").append(Strings.nz(profile.getSummary())).append('\n');
        sb.append("Location: ").append(Strings.nz(profile.getLocation())).append('\n');
        sb.append("Remote preference: ").append(profile.getRemotePref()).append('\n');
        if (profile.getSalaryMin() != null && profile.getSalaryMin() > 0) {
            sb.append("Current salary expectation: ")
                    .append(profile.getSalaryMin())
                    .append('\n');
        }
        if (cv != null) {
            sb.append("Skills: ").append(Strings.nz(cv.getSkills())).append('\n');
            sb.append("Experience: ")
                    .append(Strings.truncate(Strings.nz(cv.getExperience()), 2000))
                    .append('\n');
            sb.append("Education: ").append(Strings.nz(cv.getEducation())).append('\n');
            sb.append("Certifications: ")
                    .append(Strings.nz(cv.getCertifications()))
                    .append('\n');
        }
        return sb.toString();
    }

    private SalaryEstimate parseEstimate(String json) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(json);
            String currency = node.path("currency").asText("EUR");
            double low = node.path("low").asDouble(0);
            double median = node.path("median").asDouble(0);
            double high = node.path("high").asDouble(0);
            String confidence = node.path("confidence").asText("medium");
            var factors = new java.util.ArrayList<String>();
            node.path("factors").forEach(f -> factors.add(f.asText()));
            String marketNote = node.path("marketNote").asText("");
            return new SalaryEstimate(
                    currency, (int) low, (int) median, (int) high, confidence, factors, marketNote, null);
        } catch (Exception e) {
            return SalaryEstimate.empty("Could not parse salary estimate.");
        }
    }

    public record SalaryEstimate(
            String currency,
            Integer low,
            Integer median,
            Integer high,
            String confidence,
            java.util.List<String> factors,
            String marketNote,
            String error) {

        public static SalaryEstimate empty(String error) {
            return new SalaryEstimate(null, null, null, null, null, java.util.List.of(), "", error);
        }

        public boolean hasEstimate() {
            return median != null && median > 0;
        }
    }
}
