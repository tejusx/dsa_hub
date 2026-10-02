package dsa_hub_backend;

import dsa_hub_backend.controller.SheetController;
import dsa_hub_backend.dto.SheetProblemResponse;
import dsa_hub_backend.service.SheetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class SheetControllerApiTest {

    private final SheetService sheetService = mock(SheetService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new SheetController(sheetService)).build();
    }

    @Test
    void striverProblemsReturnOrderedOccurrenceMetadataAndExpectedArrayFaqs() throws Exception {
        when(sheetService.getProblemResponsesForSheet("striver-a2z")).thenReturn(striverOccurrences());

        JsonNode records = objectMapper.readTree(mockMvc.perform(get("/api/sheets/striver-a2z/problems"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        assertEquals(402, records.size());
        List<String> topLevelSections = new ArrayList<>();
        List<String> faqProblemIds = new ArrayList<>();
        List<String> faqTitles = new ArrayList<>();
        int previousPosition = 0;
        for (JsonNode record : records) {
            int position = record.get("position").asInt();
            assertEquals(previousPosition + 1, position, "occurrence positions should be 1 through 402");
            previousPosition = position;
            assertTrue(record.hasNonNull("occurrenceId"));
            assertTrue(record.hasNonNull("section"));
            assertTrue(record.has("sectionPath"));
            assertTrue(record.get("sectionPath").size() >= 1);
            assertTrue(record.has("topic"));
            assertTrue(record.has("links"));
            String section = record.get("section").asText();
            assertEquals(section, record.get("sectionPath").get(0).asText());
            if (!topLevelSections.contains(section)) topLevelSections.add(section);
            if ("Arrays".equals(section) && "FAQs(Medium)".equals(record.get("topic").asText())) {
                faqProblemIds.add(record.get("problemId").asText());
                faqTitles.add(record.get("title").asText());
                assertEquals("FAQs(Medium)", record.get("sectionPath").get(1).asText());
            }
        }

        assertEquals(19, topLevelSections.size());
        assertEquals(List.of(
                "lc-169", "p-leaders-in-an-array-079730b384d7", "lc-2149", "lc-54",
                "p-pascal-s-triangle-i-b8059988ebce", "lc-119", "p-pascal-s-triangle-iii-2a17313e6f72",
                "lc-48", "lc-73", "lc-1", "lc-15", "lc-18", "lc-75",
                "p-kadane-s-algorithm-3f28a16b7c23", "lc-31"
        ), faqProblemIds);
        assertEquals(List.of(
                "Majority Element", "Leaders in an Array", "Rearrange Array Elements by Sign",
                "Spiral Matrix", "Pascal's Triangle I", "Pascal's Triangle II", "Pascal's Triangle III",
                "Rotate Image", "Set Matrix Zeroes", "Two Sum", "3Sum", "4Sum", "Sort Colors",
                "Kadane's Algorithm", "Next Permutation"
        ), faqTitles);
    }

    @Test
    void anotherSheetStillReturnsProblemFieldsAndItsOwnOccurrenceMetadata() throws Exception {
        when(sheetService.getProblemResponsesForSheet("neetcode-150")).thenReturn(List.of(
                new SheetProblemResponse("lc-1", "Two Sum", "1", "two-sum", "Easy", false,
                        true, true, 9001L, 1, "Arrays", List.of("Arrays", "Hashing"), "Hashing", List.of())
        ));

        JsonNode records = objectMapper.readTree(mockMvc.perform(get("/api/sheets/neetcode-150/problems"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        assertEquals(1, records.size());
        assertEquals("lc-1", records.get(0).get("problemId").asText());
        assertEquals("Two Sum", records.get(0).get("title").asText());
        assertEquals(1, records.get(0).get("position").asInt());
        assertEquals("Hashing", records.get(0).get("topic").asText());
        assertEquals("Hashing", records.get(0).get("sectionPath").get(1).asText());
    }

    private List<SheetProblemResponse> striverOccurrences() {
        List<SheetProblemResponse> records = new ArrayList<>(402);
        List<String> faqIds = List.of(
                "lc-169", "p-leaders-in-an-array-079730b384d7", "lc-2149", "lc-54",
                "p-pascal-s-triangle-i-b8059988ebce", "lc-119", "p-pascal-s-triangle-iii-2a17313e6f72",
                "lc-48", "lc-73", "lc-1", "lc-15", "lc-18", "lc-75",
                "p-kadane-s-algorithm-3f28a16b7c23", "lc-31"
        );
        List<String> faqTitles = List.of(
                "Majority Element", "Leaders in an Array", "Rearrange Array Elements by Sign",
                "Spiral Matrix", "Pascal's Triangle I", "Pascal's Triangle II", "Pascal's Triangle III",
                "Rotate Image", "Set Matrix Zeroes", "Two Sum", "3Sum", "4Sum", "Sort Colors",
                "Kadane's Algorithm", "Next Permutation"
        );

        for (int position = 1; position <= 402; position++) {
            boolean faq = position >= 77 && position <= 91;
            String section = faq ? "Arrays" : "Section " + (1 + (position % 18));
            String topic = faq ? "FAQs(Medium)" : "Topic";
            String problemId = faq ? faqIds.get(position - 77) : "striver-problem-" + position;
            String title = faq ? faqTitles.get(position - 77) : "Problem " + position;
            records.add(new SheetProblemResponse(problemId, title, null, null, "Medium", null,
                    null, null, 10000L + position, position, section, List.of(section, topic), topic, List.of()));
        }
        return records;
    }
}
