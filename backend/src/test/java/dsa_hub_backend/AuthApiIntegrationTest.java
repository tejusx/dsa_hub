package dsa_hub_backend;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import dsa_hub_backend.entity.User;
import dsa_hub_backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void registrationLoginAndCurrentUserRequireAValidJwt() throws Exception {
        String email = "auth-check-" + UUID.randomUUID() + "@example.invalid";
        String password = "correct horse battery staple";
        String registerBody = """
                {"email":"%s","password":"%s","displayName":"Auth Check"}
                """.formatted(email.toUpperCase(), password);

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertNotEquals(password, user.getPasswordHash());
        assertTrue(user.getPasswordHash().startsWith("$2"));
        assertTrue(passwordEncoder.matches(password, user.getPasswordHash()));
        assertFalse(registerResult.getResponse().getContentAsString().contains(user.getPasswordHash()));

        Boolean settingsCreated = jdbcTemplate.queryForObject(
                "SELECT sync_progress_across_sheets FROM user_settings WHERE user_id = ?",
                Boolean.class,
                user.getUserId());
        assertEquals(Boolean.TRUE, settingsCreated);

        String duplicateBody = """
                {"email":"%s","password":"%s","displayName":"Auth Check"}
                """.formatted(email.toUpperCase(), password);
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(duplicateBody))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","password":"wrong password"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode loginResponse = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = loginResponse.get("accessToken").asText();
        assertNotNull(token);
        assertEquals("Bearer", loginResponse.get("tokenType").asText());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.displayName").value("Auth Check"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
}
