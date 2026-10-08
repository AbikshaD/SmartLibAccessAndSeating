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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import library_management.model.Announcement;
import library_management.model.User;
import library_management.repository.AnnouncementRepository;
import library_management.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AnnouncementManagementTests {

    private static final String TEST_DATABASE = "announcement_tests_"
            + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void configureMongoDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.database", () -> TEST_DATABASE);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminId;
    private String studentId;
    private String adminToken;
    private String studentToken;

    @BeforeEach
    void setUp() throws Exception {
        announcementRepository.deleteAll();

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        adminId = "ann-admin-" + suffix;
        studentId = "ann-student-" + suffix;

        saveUser(adminId, "ADMIN");
        saveUser(studentId, "STUDENT");

        adminToken = login(adminId);
        studentToken = login(studentId);
    }

    @AfterEach
    void tearDown() {
        announcementRepository.deleteAll();
        userRepository.findByStudentId(adminId).ifPresent(userRepository::delete);
        userRepository.findByStudentId(studentId).ifPresent(userRepository::delete);
    }

    private void saveUser(String id, String role) {
        User user = new User();
        user.setStudentId(id);
        user.setName(id);
        user.setEmail(id + "@example.com");
        user.setPassword(passwordEncoder.encode("ann-test-password"));
        user.setRole(role);
        userRepository.save(user);
    }

    private String login(String id) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"" + id + "\",\"password\":\"ann-test-password\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return response.replaceFirst(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }

    @Test
    void adminCanPostAnnouncementAndStudentCanViewIt() throws Exception {
        // Admin posts announcement
        String postBody = """
                {
                    "title": "Library Will Close at 6:00 PM Today",
                    "content": "Due to unexpected maintenance, the library will close at 6:00 PM today.",
                    "type": "CLOSURE",
                    "priority": "HIGH"
                }
                """;

        mockMvc.perform(post("/announcements")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Library Will Close at 6:00 PM Today"))
                .andExpect(jsonPath("$.type").value("CLOSURE"))
                .andExpect(jsonPath("$.priority").value("HIGH"));

        // Student can view the announcements
        mockMvc.perform(get("/announcements")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Library Will Close at 6:00 PM Today"));
    }

    @Test
    void studentCannotPostAnnouncement() throws Exception {
        String postBody = """
                {
                    "title": "Student Trying to Post",
                    "content": "This should be forbidden.",
                    "type": "GENERAL",
                    "priority": "NORMAL"
                }
                """;

        mockMvc.perform(post("/announcements")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotDeleteAnnouncement() throws Exception {
        Announcement announcement = announcementRepository.save(new Announcement(
                "Notice", "Sample content", "GENERAL", "NORMAL", adminId));

        mockMvc.perform(delete("/announcements/" + announcement.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanDeleteAnnouncement() throws Exception {
        Announcement announcement = announcementRepository.save(new Announcement(
                "Notice to Delete", "Content", "GENERAL", "NORMAL", adminId));

        mockMvc.perform(delete("/announcements/" + announcement.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/announcements")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
