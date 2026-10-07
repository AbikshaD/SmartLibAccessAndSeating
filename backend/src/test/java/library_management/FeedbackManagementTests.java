package library_management;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import library_management.model.User;
import library_management.repository.FeedbackRepository;
import library_management.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class FeedbackManagementTests {

        private static final String TEST_DATABASE = "feedback_tests_"
            + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void configureMongoDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.database", () -> TEST_DATABASE);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String adminId;
    private String studentId;
    private String otherStudentId;
    private String adminToken;
    private String studentToken;
    private String otherStudentToken;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        adminId = "feedback-admin-" + suffix;
        studentId = "feedback-student-" + suffix;
        otherStudentId = "feedback-other-" + suffix;
        saveUser(adminId, "ADMIN");
        saveUser(studentId, "STUDENT");
        saveUser(otherStudentId, "STUDENT");
        adminToken = login(adminId);
        studentToken = login(studentId);
        otherStudentToken = login(otherStudentId);
    }

    @AfterEach
    void cleanUp() {
        feedbackRepository.deleteAll();
        userRepository.findByStudentId(adminId).ifPresent(userRepository::delete);
        userRepository.findByStudentId(studentId).ifPresent(userRepository::delete);
        userRepository.findByStudentId(otherStudentId).ifPresent(userRepository::delete);
    }

    @Test
    void studentsCanSubmitAndReadOnlyTheirFeedbackAndAdminsCanReviewIt() throws Exception {
        MvcResult created = mockMvc.perform(post("/feedback")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feedbackJson("SEATING", 4, otherStudentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value(studentId))
                .andExpect(jsonPath("$.category").value("SEATING"))
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.adminResponse").doesNotExist())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();
        String feedbackId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(get("/feedback/my").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(feedbackId));
        mockMvc.perform(get("/feedback/" + feedbackId)
                        .header("Authorization", "Bearer " + otherStudentToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/feedback/" + feedbackId)
                        .header("Authorization", "Bearer " + otherStudentToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/feedback").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/feedback").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(studentId));
        mockMvc.perform(put("/feedback/" + feedbackId + "/respond")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"response\":\"We will add charging points.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminResponse").value("We will add charging points."))
                .andExpect(jsonPath("$.status").value("REVIEWED"));
        mockMvc.perform(put("/feedback/" + feedbackId + "/resolve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        mockMvc.perform(delete("/feedback/" + feedbackId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void feedbackRejectsInvalidRatingAndUnknownCategory() throws Exception {
        mockMvc.perform(post("/feedback")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feedbackJson("BOOKING", 0, studentId)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/feedback")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feedbackJson("RANDOM", 4, studentId)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/feedback")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feedbackJson("OTHER", 5, studentId)))
                .andExpect(status().isForbidden());
    }

    private void saveUser(String id, String role) {
        User user = new User();
        user.setStudentId(id);
        user.setName(id);
        user.setEmail(id + "@example.com");
        user.setPassword(passwordEncoder.encode("feedback-test-password"));
        user.setRole(role);
        userRepository.save(user);
    }

    private String login(String id) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"" + id + "\",\"password\":\"feedback-test-password\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return response.replaceFirst(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }

    private String feedbackJson(String category, int rating, String claimedStudentId) {
        return "{\"category\":\"" + category + "\",\"rating\":" + rating
                + ",\"comment\":\"Library feedback\",\"studentId\":\"" + claimedStudentId + "\"}";
    }
}