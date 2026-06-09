package de.bewerbungsatze.cv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bewerbungsatze.ai.LlmService;
import de.bewerbungsatze.ai.model.ChatRequest;
import de.bewerbungsatze.common.json.JsonExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Wandelt extrahierten CV-/Projektlisten-Text via LLM in strukturierte Daten um.
 */
@Service
public class CvParsingService {

    private static final String CV_SYSTEM = """
            Du bist ein Parser für Lebensläufe. Extrahiere die Angaben aus dem Text und
            antworte ausschließlich mit gültigem JSON in exakt dieser Struktur:
            {
              "experience":     [{"title":"","company":"","period":"","description":""}],
              "education":       [{"degree":"","institution":"","period":""}],
              "skills":          ["..."],
              "languages":       [{"language":"","level":""}],
              "certifications":  ["..."]
            }
            Erfinde nichts. Lass unbekannte Felder leer. Keine Erklärungen, nur JSON.
            """;

    private static final String PROJECTS_SYSTEM = """
            Du extrahierst eine Projektliste. Antworte ausschließlich mit gültigem JSON-Array:
            [{"title":"","role":"","period":"","tech":["..."],"description":""}]
            Erfinde nichts. Keine Erklärungen, nur JSON.
            """;

    private final LlmService llmService;
    private final ObjectMapper objectMapper;
    private final CvStructuredRepository cvRepository;
    private final ProjectRepository projectRepository;

    public CvParsingService(LlmService llmService,
                            ObjectMapper objectMapper,
                            CvStructuredRepository cvRepository,
                            ProjectRepository projectRepository) {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
        this.cvRepository = cvRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public CvStructured parseCv(UUID userId, UUID documentId, String text) {
        String json = callJson(userId, CV_SYSTEM, text);
        JsonNode node = readTree(json);

        CvStructured cv = cvRepository.findByUserId(userId)
                .orElseGet(() -> new CvStructured(userId, documentId));
        cv.setDocumentId(documentId);
        cv.setExperience(arrayOrEmpty(node, "experience"));
        cv.setEducation(arrayOrEmpty(node, "education"));
        cv.setSkills(arrayOrEmpty(node, "skills"));
        cv.setLanguages(arrayOrEmpty(node, "languages"));
        cv.setCertifications(arrayOrEmpty(node, "certifications"));
        return cvRepository.save(cv);
    }

    @Transactional
    public List<Project> parseProjects(UUID userId, String text) {
        String json = callJson(userId, PROJECTS_SYSTEM, text);
        JsonNode node = readTree(json);
        if (!node.isArray()) {
            throw new IllegalStateException("Projektliste konnte nicht als Array geparst werden");
        }
        projectRepository.deleteByUserId(userId);
        List<Project> result = new ArrayList<>();
        for (JsonNode item : node) {
            String title = item.path("title").asText("").strip();
            if (title.isEmpty()) {
                continue;
            }
            Project p = new Project(userId, title);
            p.setRole(emptyToNull(item.path("role").asText("")));
            p.setPeriod(emptyToNull(item.path("period").asText("")));
            p.setDescription(emptyToNull(item.path("description").asText("")));
            p.setTech(toStringArray(item.path("tech")));
            result.add(projectRepository.save(p));
        }
        return result;
    }

    private String callJson(UUID userId, String system, String text) {
        var result = llmService.chat(userId, new ChatRequest(
                List.of(de.bewerbungsatze.ai.model.ChatMessage.system(system),
                        de.bewerbungsatze.ai.model.ChatMessage.user(text)),
                0.0, 4096));
        return JsonExtractor.extract(result.content());
    }

    private JsonNode readTree(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException("LLM-Antwort war kein gültiges JSON", e);
        }
    }

    private String arrayOrEmpty(JsonNode node, String field) {
        JsonNode child = node.path(field);
        return child.isArray() ? child.toString() : "[]";
    }

    private String[] toStringArray(JsonNode node) {
        if (!node.isArray()) {
            return new String[0];
        }
        List<String> values = new ArrayList<>();
        node.forEach(n -> values.add(n.asText()));
        return values.toArray(new String[0]);
    }

    private String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
