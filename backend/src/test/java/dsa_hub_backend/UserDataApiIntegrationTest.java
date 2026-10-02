package dsa_hub_backend;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserDataApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void publicContentWorksWithoutAuthenticationWhileUserDataStaysProtected() throws Exception {
        mockMvc.perform(get("/api/sheets"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/problems"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/sheets/striver-a2z/problem-occurrences")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/progress"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/problems/lc-1/attempts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sheetProblemOccurrencesIncludeSourceGroupsLinksAndRepeatedAppearances() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/sheets/striver-a2z/problem-occurrences"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode occurrences = objectMapper.readTree(result.getResponse().getContentAsString());
        int repeatedProblemCount = 0;
        long previousOccurrenceId = 0;
        boolean foundProblemLink = false;
        for (JsonNode occurrence : occurrences) {
            long currentOccurrenceId = occurrence.get("occurrenceId").asLong();
            assertTrue(currentOccurrenceId >= previousOccurrenceId, "occurrences should follow source order");
            previousOccurrenceId = currentOccurrenceId;
            assertTrue(occurrence.has("section"));
            assertTrue(occurrence.has("subsection"));
            JsonNode problem = occurrence.get("problem");
            if ("lc-8".equals(problem.get("problemId").asText())) {
                repeatedProblemCount++;
            }
            if ("lc-1".equals(problem.get("problemId").asText())
                    && occurrence.get("links").size() > 0) {
                foundProblemLink = true;
            }
        }

        assertEquals(2, repeatedProblemCount, "the source intentionally places lc-8 twice");
        assertTrue(foundProblemLink, "problem links should be returned with the occurrence");
        mockMvc.perform(get("/api/sheets/missing-sheet/problem-occurrences"))
                .andExpect(status().isNotFound());
    }

    @Test
    void userSettingsProgressSyncAttemptsAndIsolationWorkTogether() throws Exception {
        mockMvc.perform(get("/api/settings")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/progress")).andExpect(status().isUnauthorized());

        String userAToken = registerAndLogin("progress-a-" + UUID.randomUUID() + "@example.invalid");
        String userBToken = registerAndLogin("progress-b-" + UUID.randomUUID() + "@example.invalid");

        mockMvc.perform(get("/api/settings").header("Authorization", bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncProgressAcrossSheets").value(true));

        mockMvc.perform(patch("/api/settings")
                        .header("Authorization", bearer(userAToken))
                        .contentType("application/json")
                        .content("{\"syncProgressAcrossSheets\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncProgressAcrossSheets").value(false));

        String solvedProgress = """
                {"solved":true,"isBookmarked":true,"notes":"global progress","revisionStatus":"learning","userDifficulty":"Easy"}
                """;
        mockMvc.perform(put("/api/progress/lc-1")
                        .header("Authorization", bearer(userAToken))
                        .contentType("application/json")
                        .content(solvedProgress))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solved").value(true))
                .andExpect(jsonPath("$.isBookmarked").value(true))
                .andExpect(jsonPath("$.solvedAt").exists());

        String unsolvedSheetProgress = """
                {"solved":false,"isBookmarked":false,"notes":"sheet only","revisionStatus":"new","userDifficulty":"Medium"}
                """;
        mockMvc.perform(put("/api/sheets/striver-a2z/progress/lc-1")
                        .header("Authorization", bearer(userAToken))
                        .contentType("application/json")
                        .content(unsolvedSheetProgress))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solved").value(false))
                .andExpect(jsonPath("$.notes").value("sheet only"));

        mockMvc.perform(get("/api/progress/lc-1").header("Authorization", bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solved").value(true))
                .andExpect(jsonPath("$.notes").value("global progress"));
        MvcResult globalListResult = mockMvc.perform(get("/api/progress")
                        .header("Authorization", bearer(userAToken)))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("lc-1", objectMapper.readTree(globalListResult.getResponse().getContentAsString())
                .get(0).get("problemId").asText());

        mockMvc.perform(patch("/api/settings")
                        .header("Authorization", bearer(userAToken))
                        .contentType("application/json")
                        .content("{\"sync_progress_across_sheets\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncProgressAcrossSheets").value(true));

        mockMvc.perform(get("/api/sheets/striver-a2z/progress/lc-1")
                        .header("Authorization", bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solved").value(true))
                .andExpect(jsonPath("$.notes").value("global progress"));
        mockMvc.perform(get("/api/sheets/neetcode-150/progress/lc-1")
                        .header("Authorization", bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solved").value(true));
        MvcResult sheetListResult = mockMvc.perform(get("/api/sheets/striver-a2z/progress")
                        .header("Authorization", bearer(userAToken)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode sheetProgressList = objectMapper.readTree(sheetListResult.getResponse().getContentAsString());
        assertTrue(findProblem(sheetProgressList, "lc-1").get("solved").asBoolean());

        String progressFromSheet = """
                {"solved":true,"isBookmarked":true,"notes":"updated from a sheet","revisionStatus":"mastered","userDifficulty":"Hard","lastAttemptedAt":"2026-10-01T12:00:00Z"}
                """;
        mockMvc.perform(put("/api/sheets/striver-a2z/progress/lc-1")
                        .header("Authorization", bearer(userAToken))
                        .contentType("application/json")
                        .content(progressFromSheet))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisionStatus").value("mastered"));
        mockMvc.perform(get("/api/progress/lc-1").header("Authorization", bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("updated from a sheet"))
                .andExpect(jsonPath("$.userDifficulty").value("Hard"));

        mockMvc.perform(patch("/api/settings")
                        .header("Authorization", bearer(userAToken))
                        .contentType("application/json")
                        .content("{\"syncProgressAcrossSheets\":false}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/sheets/striver-a2z/progress/lc-1")
                        .header("Authorization", bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solved").value(false))
                .andExpect(jsonPath("$.notes").value("sheet only"));

        mockMvc.perform(post("/api/problems/lc-1/attempts")
                        .header("Authorization", bearer(userAToken))
                        .contentType("application/json")
                        .content("""
                                {"sheet_id":"striver-a2z","coding_platform":"LeetCode","outcome":"solved","notes":"logged separately"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome").value("solved"))
                .andExpect(jsonPath("$.userId").doesNotExist());

        mockMvc.perform(put("/api/sheets/striver-a2z/progress/lc-1")
                        .header("Authorization", bearer(userAToken))
                        .contentType("application/json")
                        .content(unsolvedSheetProgress))
                .andExpect(status().isOk());
        MvcResult attemptsResult = mockMvc.perform(get("/api/problems/lc-1/attempts")
                        .header("Authorization", bearer(userAToken)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode attempts = objectMapper.readTree(attemptsResult.getResponse().getContentAsString());
        assertEquals(1, attempts.size());
        assertEquals("logged separately", attempts.get(0).get("notes").asText());

        mockMvc.perform(get("/api/progress/lc-1").header("Authorization", bearer(userBToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solved").value(false))
                .andExpect(jsonPath("$.notes").doesNotExist());
        mockMvc.perform(get("/api/problems/lc-1/attempts").header("Authorization", bearer(userBToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").doesNotExist());
        mockMvc.perform(get("/api/settings").header("Authorization", bearer(userBToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncProgressAcrossSheets").value(true));

        mockMvc.perform(get("/api/sheets/missing-sheet/progress/lc-1")
                        .header("Authorization", bearer(userAToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/sheets/striver-a2z/progress/not-a-member")
                        .header("Authorization", bearer(userAToken)))
                .andExpect(status().isNotFound());

        assertTrue(attempts.get(0).get("attemptId").isNumber());
    }

    @Test
    void guestProgressMigrationIsAuthenticatedValidatedAndMergesSafely() throws Exception {
        String request = """
                {"syncProgressAcrossSheets":true,"problemProgress":[
                  {"problemId":"lc-1","solved":false,"notes":"guest note","isBookmarked":false,"revisionStatus":"review","userDifficulty":"Medium"}
                ],"sheetProblemProgress":[]}
                """;
        mockMvc.perform(post("/api/progress/migrate-guest")
                        .contentType("application/json")
                        .content(request))
                .andExpect(status().isUnauthorized());

        String token = registerAndLogin("guest-migration-" + UUID.randomUUID() + "@example.invalid");
        String auth = bearer(token);

        mockMvc.perform(put("/api/progress/lc-1")
                        .header("Authorization", auth)
                        .contentType("application/json")
                        .content("{\"solved\":true,\"isBookmarked\":true,\"notes\":\"account note\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/progress/migrate-guest")
                        .header("Authorization", auth)
                        .contentType("application/json")
                        .content("""
                                {"syncProgressAcrossSheets":true,"problemProgress":[
                                  {"problemId":"lc-1","solved":false,"notes":"guest note","isBookmarked":false,"revisionStatus":"review","userDifficulty":"Medium"}
                                ],"sheetProblemProgress":[]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.migratedProblems").value(1))
                .andExpect(jsonPath("$.migratedSheetProblems").value(0));

        mockMvc.perform(get("/api/progress/lc-1").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solved").value(true))
                .andExpect(jsonPath("$.isBookmarked").value(true))
                .andExpect(jsonPath("$.notes").value("account note"))
                .andExpect(jsonPath("$.revisionStatus").value("review"))
                .andExpect(jsonPath("$.userDifficulty").value("Medium"));

        mockMvc.perform(post("/api/progress/migrate-guest")
                        .header("Authorization", auth)
                        .contentType("application/json")
                        .content("""
                                {"syncProgressAcrossSheets":true,"problemProgress":[
                                  {"problemId":"lc-2","solved":true,"isBookmarked":false},
                                  {"problemId":"missing-problem","solved":true,"isBookmarked":false}
                                ],"sheetProblemProgress":[]}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/progress/lc-2").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solved").value(false));

        mockMvc.perform(post("/api/progress/migrate-guest")
                        .header("Authorization", auth)
                        .contentType("application/json")
                        .content("""
                                {"syncProgressAcrossSheets":false,"problemProgress":[],"sheetProblemProgress":[]}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/settings")
                        .header("Authorization", auth)
                        .contentType("application/json")
                        .content("{\"syncProgressAcrossSheets\":false}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/progress/migrate-guest")
                        .header("Authorization", auth)
                        .contentType("application/json")
                        .content("""
                                {"syncProgressAcrossSheets":false,"problemProgress":[],"sheetProblemProgress":[
                                  {"sheetId":"missing-sheet","problemId":"lc-1","solved":true,"isBookmarked":false}
                                ]}
                                """))
                .andExpect(status().isBadRequest());
    }

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","password":"progress test password","displayName":"Progress Test"}
                                """.formatted(email)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","password":"progress test password"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private JsonNode findProblem(JsonNode progressList, String problemId) {
        for (JsonNode progress : progressList) {
            if (problemId.equals(progress.get("problemId").asText())) {
                return progress;
            }
        }
        throw new AssertionError("Problem progress not found for " + problemId);
    }
}
