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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import library_management.model.User;
import library_management.repository.MaintenanceTicketRepository;
import library_management.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MaintenanceManagementTests {

    private static final String TEST_DATABASE = "maintenance_tests_"
            + UUID.randomUUID().toString().replace("-", "");
    private static final String PASSWORD = "maintenance-test-password";

    @DynamicPropertySource
    static void configureMongoDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.database", () -> TEST_DATABASE);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MaintenanceTicketRepository ticketRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String adminId;
    private String studentId;
    private String networkId;
    private String electricalId;
    private String adminToken;
    private String studentToken;
    private String networkToken;
    private String electricalToken;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        adminId = "maintenance-admin-" + suffix;
        studentId = "maintenance-student-" + suffix;
        networkId = "maintenance-network-" + suffix;
        electricalId = "maintenance-electrical-" + suffix;

        saveUser(adminId, "ADMIN");
        saveUser(studentId, "STUDENT");
        saveUser(networkId, "MAINTENANCE", "NETWORK");
        saveUser(electricalId, "MAINTENANCE", "ELECTRICAL");
        adminToken = login(adminId);
        studentToken = login(studentId);
        networkToken = login(networkId);
        electricalToken = login(electricalId);
    }

    @AfterEach
    void cleanUp() {
        ticketRepository.deleteAll();
        userRepository.findByStudentId(adminId).ifPresent(userRepository::delete);
        userRepository.findByStudentId(studentId).ifPresent(userRepository::delete);
        userRepository.findByStudentId(networkId).ifPresent(userRepository::delete);
        userRepository.findByStudentId(electricalId).ifPresent(userRepository::delete);
    }

    @Test
    void adminCanCreateListAssignAndDeactivateMaintenanceAccounts() throws Exception {
        String newId = "maintenance-new-" + UUID.randomUUID().toString().substring(0, 8);
        String newEmail = newId + "@example.com";

        MvcResult created = mockMvc.perform(post("/users/maintenance")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maintenanceUserJson(newId, newEmail, "HOUSEKEEPING")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MAINTENANCE"))
                .andExpect(jsonPath("$.maintenanceTeam").value("HOUSEKEEPING"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();
        String createdToken = loginFromResponse(created, PASSWORD);

        mockMvc.perform(get("/users/maintenance")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.studentId=='" + newId + "')]").exists());

        mockMvc.perform(put("/users/" + newId + "/maintenance-team")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"team\":\"IT_SUPPORT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maintenanceTeam").value("IT_SUPPORT"));

        mockMvc.perform(get("/maintenance/dashboard")
                        .header("Authorization", "Bearer " + createdToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.team").value("IT_SUPPORT"));

        mockMvc.perform(put("/users/" + newId + "/active")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(newId, PASSWORD)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/maintenance/dashboard").header("Authorization", "Bearer " + createdToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/users/" + newId + "/active")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":true}"))
                .andExpect(status().isOk());
        assertTrue(userRepository.findByStudentId(newId).orElseThrow().isActive());

        userRepository.findByStudentId(newId).ifPresent(userRepository::delete);
    }

    @Test
    void maintenanceDashboardAndTicketActionsAreRestrictedToTheAssignedTeam() throws Exception {
        String networkOpenId = createTicket("NETWORK", "Router outage", "Second", "HIGH");
        String networkInProgressId = createTicket("NETWORK", "Wi-Fi access point issue", "First", "MEDIUM");
        String networkResolvedId = createTicket("NETWORK", "Switch replacement", "Ground", "LOW");
        String electricalId = createTicket("ELECTRICAL", "Lighting fault", "Third", "URGENT");

        mockMvc.perform(put("/maintenance/tickets/" + networkInProgressId + "/status")
                        .header("Authorization", "Bearer " + networkToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mockMvc.perform(put("/maintenance/tickets/" + networkResolvedId + "/status")
                        .header("Authorization", "Bearer " + networkToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/maintenance/dashboard?team=ELECTRICAL")
                        .header("Authorization", "Bearer " + networkToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.team").value("NETWORK"))
                .andExpect(jsonPath("$.openCount").value(1))
                .andExpect(jsonPath("$.inProgressCount").value(1))
                .andExpect(jsonPath("$.resolvedCount").value(1))
                .andExpect(jsonPath("$.tickets.length()").value(3))
                .andExpect(jsonPath("$.tickets[?(@.team=='ELECTRICAL')]").doesNotExist());

        mockMvc.perform(get("/maintenance/dashboard")
                        .header("Authorization", "Bearer " + electricalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.team").value("ELECTRICAL"))
                .andExpect(jsonPath("$.tickets.length()").value(1))
                .andExpect(jsonPath("$.tickets[0].id").value(electricalId));

        mockMvc.perform(get("/maintenance/tickets/" + electricalId)
                        .header("Authorization", "Bearer " + networkToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/maintenance/tickets/" + electricalId + "/status")
                        .header("Authorization", "Bearer " + networkToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/maintenance/tickets/" + networkOpenId)
                        .header("Authorization", "Bearer " + networkToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Router outage"))
                .andExpect(jsonPath("$.floor").value("Second"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.submittedAt").isNotEmpty());

        mockMvc.perform(get("/maintenance/dashboard").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/maintenance/tickets")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ticketJson("NETWORK", "Test", "Ground", "LOW")))
                .andExpect(status().isForbidden());
    }

    @Test
    void maintenanceLoginResponseIdentifiesRoleAndAssignedTeam() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(networkId, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MAINTENANCE"))
                .andExpect(jsonPath("$.team").value("NETWORK"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    private String createTicket(String team, String description, String floor, String priority) throws Exception {
        MvcResult result = mockMvc.perform(post("/maintenance/tickets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ticketJson(team, description, floor, priority)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.submittedAt").isNotEmpty())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String ticketJson(String team, String description, String floor, String priority) {
        return "{\"team\":\"" + team + "\",\"description\":\"" + description
                + "\",\"floor\":\"" + floor + "\",\"priority\":\"" + priority + "\"}";
    }

    private String maintenanceUserJson(String id, String email, String team) {
        return "{\"studentId\":\"" + id + "\",\"name\":\"Maintenance User\",\"email\":\""
                + email + "\",\"password\":\"" + PASSWORD + "\",\"team\":\"" + team + "\"}";
    }

    private void saveUser(String id, String role) {
        saveUser(id, role, null);
    }

    private void saveUser(String id, String role, String team) {
        User user = new User();
        user.setStudentId(id);
        user.setName(id);
        user.setEmail(id + "@example.com");
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        if (team != null) {
            user.setMaintenanceTeam(library_management.model.MaintenanceTeam.valueOf(team));
        }
        userRepository.save(user);
    }

    private String login(String id) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(id, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String loginFromResponse(MvcResult created, String password) throws Exception {
        JsonNode user = objectMapper.readTree(created.getResponse().getContentAsString());
        return login(user.get("studentId").asText(), password);
    }

    private String login(String id, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(id, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String loginJson(String id, String password) {
        return "{\"studentId\":\"" + id + "\",\"password\":\"" + password + "\"}";
    }
}
