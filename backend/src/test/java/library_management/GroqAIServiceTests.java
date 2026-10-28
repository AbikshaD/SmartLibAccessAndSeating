package library_management;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import library_management.dto.AIClassificationResult;
import library_management.model.MaintenanceTeam;
import library_management.model.MaintenanceTicket;
import library_management.service.GroqAIService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroqAIServiceTests {

    private GroqAIService groqAIService;

    @BeforeEach
    void setUp() {
        groqAIService = new GroqAIService("https://api.groq.com/openai/v1/chat/completions",
                "openai/gpt-oss-120b", "test-key");
    }

    @Test
    void parseAndValidateNetworkClassification() {
        String json = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"category\\":\\"NETWORK\\",\\"priority\\":\\"HIGH\\",\\"reason\\":\\"Wi-Fi outage on second floor prevents student research.\\"}"
                      }
                    }
                  ]
                }
                """;

        Optional<AIClassificationResult> result = groqAIService.parseAndValidate(json);
        assertTrue(result.isPresent());
        assertEquals(MaintenanceTeam.NETWORK, result.get().category());
        assertEquals(MaintenanceTicket.Priority.HIGH, result.get().priority());
        assertEquals("Wi-Fi outage on second floor prevents student research.", result.get().reason());
    }

    @Test
    void parseAndValidateElectricalClassification() {
        String json = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"category\\":\\"ELECTRICAL\\",\\"priority\\":\\"MEDIUM\\",\\"reason\\":\\"Broken reading lamp on study desk.\\"}"
                      }
                    }
                  ]
                }
                """;

        Optional<AIClassificationResult> result = groqAIService.parseAndValidate(json);
        assertTrue(result.isPresent());
        assertEquals(MaintenanceTeam.ELECTRICAL, result.get().category());
        assertEquals(MaintenanceTicket.Priority.MEDIUM, result.get().priority());
        assertEquals("Broken reading lamp on study desk.", result.get().reason());
    }

    @Test
    void parseAndValidateItSupportClassification() {
        String json = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"category\\":\\"IT_SUPPORT\\",\\"priority\\":\\"HIGH\\",\\"reason\\":\\"Computer lab desktop will not boot into operating system.\\"}"
                      }
                    }
                  ]
                }
                """;

        Optional<AIClassificationResult> result = groqAIService.parseAndValidate(json);
        assertTrue(result.isPresent());
        assertEquals(MaintenanceTeam.IT_SUPPORT, result.get().category());
        assertEquals(MaintenanceTicket.Priority.HIGH, result.get().priority());
        assertEquals("Computer lab desktop will not boot into operating system.", result.get().reason());
    }

    @Test
    void parseAndValidateHousekeepingClassificationWithCodeFences() {
        String json = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "```json\\n{\\"category\\":\\"HOUSEKEEPING\\",\\"priority\\":\\"LOW\\",\\"reason\\":\\"Coffee spill next to study cubicle.\\"}\\n```"
                      }
                    }
                  ]
                }
                """;

        Optional<AIClassificationResult> result = groqAIService.parseAndValidate(json);
        assertTrue(result.isPresent());
        assertEquals(MaintenanceTeam.HOUSEKEEPING, result.get().category());
        assertEquals(MaintenanceTicket.Priority.LOW, result.get().priority());
        assertEquals("Coffee spill next to study cubicle.", result.get().reason());
    }

    @Test
    void rejectsInvalidCategory() {
        String json = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"category\\":\\"PLUMBING\\",\\"priority\\":\\"HIGH\\",\\"reason\\":\\"Pipe leak\\"}"
                      }
                    }
                  ]
                }
                """;

        Optional<AIClassificationResult> result = groqAIService.parseAndValidate(json);
        assertFalse(result.isPresent());
    }

    @Test
    void rejectsInvalidPriority() {
        String json = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"category\\":\\"NETWORK\\",\\"priority\\":\\"CRITICAL_MAX\\",\\"reason\\":\\"Router explosion\\"}"
                      }
                    }
                  ]
                }
                """;

        Optional<AIClassificationResult> result = groqAIService.parseAndValidate(json);
        assertFalse(result.isPresent());
    }

    @Test
    void rejectsMalformedJson() {
        String json = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "This is not json."
                      }
                    }
                  ]
                }
                """;

        Optional<AIClassificationResult> result = groqAIService.parseAndValidate(json);
        assertFalse(result.isPresent());
    }

    @Test
    void handlesEmptyInputGracefully() {
        assertFalse(groqAIService.parseAndValidate(null).isPresent());
        assertFalse(groqAIService.parseAndValidate("").isPresent());
        assertFalse(groqAIService.classify(null).isPresent());
        assertFalse(groqAIService.classify("").isPresent());
    }
}
