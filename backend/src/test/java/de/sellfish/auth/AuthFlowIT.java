package de.sellfish.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.sellfish.support.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class AuthFlowIT extends AbstractPostgresIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void registerLoginAndAccessProtectedRoute() throws Exception {
        String body = """
                {"email":"test@example.com","password":"supersecret1"}
                """;

        // Registrierung liefert Tokens
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode tokens = objectMapper.readTree(response);
        String accessToken = tokens.get("accessToken").asText();

        // Geschützte Route ohne Token -> 401
        mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());

        // Mit Token -> 200 + korrekte E-Mail
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));

        // Login funktioniert ebenfalls
        mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void profileGetCreatesDefaultAndUpdateWorks() throws Exception {
        String creds =
                """
                {"email":"profile@example.com","password":"supersecret1"}
                """;
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(creds))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(response).get("accessToken").asText();

        // Default-Profil wird angelegt
        mockMvc.perform(get("/api/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remotePref").value("ANY"));

        // Update inkl. Präferenz-Arrays
        String prefs =
                """
                {"desiredTitles":["Java Entwickler","Backend Engineer"],"keywords":["spring","kafka"]}
                """;
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/profile/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(prefs))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desiredTitles[0]").value("Java Entwickler"));
    }
}
