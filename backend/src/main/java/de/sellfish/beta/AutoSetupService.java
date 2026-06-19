package de.sellfish.beta;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.sellfish.ai.LlmService;
import de.sellfish.ai.model.ChatRequest;
import de.sellfish.ai.model.ChatResult;
import de.sellfish.cv.CvStructured;
import de.sellfish.cv.CvStructuredRepository;
import de.sellfish.jobs.JobSearchService;
import de.sellfish.profile.ProfileRepository;
import de.sellfish.profile.ProfileService;
import de.sellfish.profile.UserPreferences;
import de.sellfish.profile.UserProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutoSetupService {

    private static final Logger log = LoggerFactory.getLogger(AutoSetupService.class);

    private final LlmService llmService;
    private final CvStructuredRepository cvRepository;
    private final ProfileRepository profileRepository;
    private final ProfileService profileService;
    private final JobSearchService jobSearchService;
    private final ObjectMapper mapper;

    public AutoSetupService(
            LlmService llmService,
            CvStructuredRepository cvRepository,
            ProfileRepository profileRepository,
            ProfileService profileService,
            JobSearchService jobSearchService,
            ObjectMapper mapper) {
        this.llmService = llmService;
        this.cvRepository = cvRepository;
        this.profileRepository = profileRepository;
        this.profileService = profileService;
        this.jobSearchService = jobSearchService;
        this.mapper = mapper;
    }

    @Transactional
    public SetupResult run(UUID userId) {
        CvStructured cv = cvRepository.findByUserId(userId).orElse(null);
        if (cv == null) {
            return new SetupResult("no_cv", "No CV found — upload a CV first", null);
        }

        String cvData = buildCvText(cv);
        String personalMeta =
                profileRepository.findByUserId(userId).map(p -> p.getMeta()).orElse("{}");
        String json = extractProfileJson(userId, cvData, personalMeta);
        ProfileData parsed = parseProfileJson(json);

        if (parsed == null) {
            return new SetupResult("parse_failed", "LLM could not extract profile data", null);
        }

        UserProfile profile = profileService.getOrCreateProfile(userId);
        profile.setHeadline(parsed.headline());
        profile.setSummary(parsed.summary());
        profile.setLocation(parsed.location());
        if (parsed.remotePref() != null) profile.setRemotePref(parsed.remotePref());
        if (parsed.salaryMin() != null) profile.setSalaryMin(parsed.salaryMin());
        profileService.save(profile);

        UserPreferences prefs = profileService.getOrCreatePreferences(userId);
        if (parsed.titles() != null && parsed.titles().length > 0) {
            prefs.setDesiredTitles(parsed.titles());
        }
        if (parsed.keywords() != null && parsed.keywords().length > 0) {
            prefs.setKeywords(parsed.keywords());
        }
        profileService.save(prefs);

        StringBuilder summary = new StringBuilder();
        summary.append("Profile: ")
                .append(parsed.headline() != null ? "✓" : "—")
                .append(", ");
        summary.append("Titles: ")
                .append(parsed.titles() != null ? parsed.titles().length : 0)
                .append(", ");
        summary.append("Keywords: ").append(parsed.keywords() != null ? parsed.keywords().length : 0);

        jobSearchService.runForUser(userId);

        return new SetupResult("ok", summary.toString(), parsed);
    }

    private String extractProfileJson(UUID userId, String cvData, String personalMeta) {
        String prompt =
                """
                Extract structured profile data from this CV%s. Return ONLY valid JSON.

                {
                  "headline": "Senior Java Developer",
                  "summary": "Experienced backend engineer...",
                  "location": "Berlin, Germany",
                  "remotePref": "REMOTE",
                  "salaryMin": 70000,
                  "titles": ["Java Developer", "Backend Engineer", "Software Developer"],
                  "keywords": ["spring", "kafka", "docker", "kubernetes", "aws"]
                }

                CV Data:
                %s

                JSON:"""
                        .formatted(
                                personalMeta != null && !personalMeta.equals("{}")
                                        ? " and this personal context: " + personalMeta
                                        : "",
                                cvData);

        ChatResult result = llmService.chat(
                userId,
                ChatRequest.of(
                        "You extract structured data from CVs. Reply ONLY with valid JSON, no markdown.", prompt));
        String content = result.content() != null ? result.content().trim() : "";
        if (content.startsWith("```")) {
            content = content.replaceAll("```[a-z]*\n?", "")
                    .replaceAll("\n```", "")
                    .trim();
        }
        return content;
    }

    private ProfileData parseProfileJson(String json) {
        try {
            JsonNode node = mapper.readTree(json);
            String headline = text(node, "headline");
            String summary = text(node, "summary");
            String location = text(node, "location");
            String remote = text(node, "remotePref");
            Integer salary = node.has("salaryMin") ? node.get("salaryMin").asInt() : null;
            String[] titles = arr(node, "titles");
            String[] keywords = arr(node, "keywords");
            return new ProfileData(headline, summary, location, remote, salary, titles, keywords);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String buildCvText(CvStructured cv) {
        StringBuilder sb = new StringBuilder();
        sb.append("Skills: ").append(cv.getSkills()).append('\n');
        sb.append("Experience: ").append(cv.getExperience()).append('\n');
        sb.append("Education: ").append(cv.getEducation()).append('\n');
        if (cv.getLanguages() != null && !cv.getLanguages().equals("[]")) {
            sb.append("Languages: ").append(cv.getLanguages()).append('\n');
        }
        if (cv.getCertifications() != null && !cv.getCertifications().equals("[]")) {
            sb.append("Certifications: ").append(cv.getCertifications()).append('\n');
        }
        return sb.toString();
    }

    private String text(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private String[] arr(JsonNode node, String field) {
        if (!node.has(field) || !node.get(field).isArray()) return null;
        List<String> list = new ArrayList<>();
        for (JsonNode item : node.get(field)) list.add(item.asText());
        return list.toArray(new String[0]);
    }

    public record ProfileData(
            String headline,
            String summary,
            String location,
            String remotePref,
            Integer salaryMin,
            String[] titles,
            String[] keywords) {}

    public record SetupResult(String status, String message, ProfileData data) {}
}
