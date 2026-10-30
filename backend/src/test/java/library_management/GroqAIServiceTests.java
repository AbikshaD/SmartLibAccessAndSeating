package library_management;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import library_management.dto.AIClassificationResult;
import library_management.dto.BookRecommendationResult;
import library_management.exception.BookRecommendationException;
import library_management.model.Book;
import library_management.model.MaintenanceTeam;
import library_management.model.MaintenanceTicket;
import library_management.service.GroqAIService;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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

    @Test
    void bookRecommendationsReportMissingGroqConfigurationExplicitly() {
        GroqAIService unconfiguredService = new GroqAIService(
                "https://api.groq.com/openai/v1/chat/completions", "openai/gpt-oss-120b", "");

        BookRecommendationException exception = assertThrows(BookRecommendationException.class,
                () -> unconfiguredService.recommendBooks("Suggest Java books", List.of(),
                        List.of(new Book("Effective Java", "Joshua Bloch", "Computer Science",
                                null, 2018, 3, 3, "CS-01", "Java programming"))));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
    }

    @Test
    void bookRecommendationUsesConfiguredGroqEndpointAndParsesCatalogIds() {
        String endpoint = "https://api.groq.test/openai/v1/chat/completions";
        RestClient.Builder clientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(clientBuilder).build();
        GroqAIService service = new GroqAIService(endpoint, "test-model", "test-key",
                new ObjectMapper(), clientBuilder.build());
        server.expect(requestTo(endpoint))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"{\\"bookIds\\":[\\"book-1\\"]}"}}]}
                        """, MediaType.APPLICATION_JSON));

        Optional<BookRecommendationResult> result = service.recommendBooks(
                "Recommend beginner Java books", List.of(), List.of(
                        new Book("Effective Java", "Joshua Bloch", "Computer Science",
                                null, 2018, 3, 3, "CS-01", "Java programming")));

        assertTrue(result.isPresent());
        assertEquals(List.of("book-1"), result.get().bookIds());
        server.verify();
    }

    @Test
    void bookRecommendationSurfacesGroqRequestErrorsInsteadOfCallingThemConnectionFailures() {
        String endpoint = "https://api.groq.test/openai/v1/chat/completions";
        RestClient.Builder clientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(clientBuilder).build();
        GroqAIService service = new GroqAIService(endpoint, "test-model", "test-key",
                new ObjectMapper(), clientBuilder.build());
        server.expect(requestTo(endpoint))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"error":{"message":"The model does not support this response format."}}
                                """));

        BookRecommendationException exception = assertThrows(BookRecommendationException.class,
                () -> service.recommendBooks("Recommend Java books", List.of(), List.of(
                        new Book("Effective Java", "Joshua Bloch", "Computer Science",
                                null, 2018, 3, 3, "CS-01", "Java programming"))));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertTrue(exception.getMessage().contains("HTTP 400"));
        assertTrue(exception.getMessage().contains("does not support this response format"));
        assertFalse(exception.getMessage().contains("test-key"));
        server.verify();
    }
}
