package library_management;

import java.util.Optional;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import library_management.dto.AIClassificationResult;
import library_management.model.Feedback;
import library_management.model.MaintenanceTeam;
import library_management.model.MaintenanceTicket;
import library_management.model.User;
import library_management.repository.FeedbackRepository;
import library_management.repository.MaintenanceTicketRepository;
import library_management.repository.UserRepository;
import library_management.service.GroqAIService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MaintenanceTicketClassificationTests {

    private static final String TEST_DATABASE = "ticket_classification_tests_"
            + UUID.randomUUID().toString().replace("-", "");
    private static final String PASSWORD = "classification-test-pass";

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
    private FeedbackRepository feedbackRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private GroqAIService groqAIService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String adminId;
    private String maintenanceAdminId;
    private String studentId;
    private String otherStudentId;
    private String networkStaffId;
    private String electricalStaffId;

    private String adminToken;
    private String maintenanceAdminToken;
    private String studentToken;
    private String otherStudentToken;
    private String networkStaffToken;
    private String electricalStaffToken;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        adminId = "admin-" + suffix;
        maintenanceAdminId = "maint-admin-" + suffix;
        studentId = "student-" + suffix;
        otherStudentId = "other-student-" + suffix;
        networkStaffId = "net-staff-" + suffix;
        electricalStaffId = "elec-staff-" + suffix;

        saveUser(adminId, "ADMIN", null);
        saveUser(maintenanceAdminId, "MAINTENANCE_ADMIN", null);
        saveUser(studentId, "STUDENT", null);
        saveUser(otherStudentId, "STUDENT", null);
        saveUser(networkStaffId, "MAINTENANCE", MaintenanceTeam.NETWORK);
        saveUser(electricalStaffId, "MAINTENANCE", MaintenanceTeam.ELECTRICAL);

        adminToken = login(adminId);
        maintenanceAdminToken = login(maintenanceAdminId);
        studentToken = login(studentId);
        otherStudentToken = login(otherStudentId);
        networkStaffToken = login(networkStaffId);
        electricalStaffToken = login(electricalStaffId);
    }

    @AfterEach
    void tearDown() {
        ticketRepository.deleteAll();
        feedbackRepository.deleteAll();
        userRepository.findByStudentId(adminId).ifPresent(userRepository::delete);
        userRepository.findByStudentId(maintenanceAdminId).ifPresent(userRepository::delete);
        userRepository.findByStudentId(studentId).ifPresent(userRepository::delete);
        userRepository.findByStudentId(otherStudentId).ifPresent(userRepository::delete);
        userRepository.findByStudentId(networkStaffId).ifPresent(userRepository::delete);
        userRepository.findByStudentId(electricalStaffId).ifPresent(userRepository::delete);
    }

    @Test
    void scenario1_submitWifiComplaint_classifiedAsNetwork() throws Exception {
        String description = "The Wi-Fi is not working on the second floor, and students cannot access online study materials.";
        when(groqAIService.classify(contains("Wi-Fi"))).thenReturn(Optional.of(
                new AIClassificationResult(MaintenanceTeam.NETWORK, MaintenanceTicket.Priority.HIGH,
                        "Wi-Fi failure prevents students from accessing study resources.")
        ));

        MvcResult result = mockMvc.perform(post("/maintenance/reports")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"" + description + "\",\"floor\":\"Second Floor\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.team").value("NETWORK"))
                .andExpect(jsonPath("$.aiCategory").value("NETWORK"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.classificationReason").value("Wi-Fi failure prevents students from accessing study resources."))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignedStaffId").value(networkStaffId))
                .andExpect(jsonPath("$.reportedBy").value(studentId))
                .andReturn();

        String ticketId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
        MaintenanceTicket saved = ticketRepository.findById(ticketId).orElseThrow();
        assertEquals(MaintenanceTeam.NETWORK, saved.getTeam());
        assertEquals(MaintenanceTicket.Priority.HIGH, saved.getPriority());
        assertEquals(networkStaffId, saved.getAssignedStaffId());
    }

    @Test
    void scenario2_submitBrokenLightComplaint_classifiedAsElectrical() throws Exception {
        String description = "The overhead tube lights in the reading area are broken and flickering constantly.";
        when(groqAIService.classify(contains("lights"))).thenReturn(Optional.of(
                new AIClassificationResult(MaintenanceTeam.ELECTRICAL, MaintenanceTicket.Priority.MEDIUM,
                        "Lighting failure in reading area requires electrician attention.")
        ));

        mockMvc.perform(post("/maintenance/reports")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"" + description + "\",\"floor\":\"First Floor\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.team").value("ELECTRICAL"))
                .andExpect(jsonPath("$.aiCategory").value("ELECTRICAL"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignedStaffId").value(electricalStaffId));
    }

    @Test
    void scenario3_submitComputerComplaint_classifiedAsItSupport() throws Exception {
        String description = "Desktop computer PC-05 in the digital lab has a blue screen and keyboard does not respond.";
        when(groqAIService.classify(contains("Desktop computer"))).thenReturn(Optional.of(
                new AIClassificationResult(MaintenanceTeam.IT_SUPPORT, MaintenanceTicket.Priority.HIGH,
                        "Digital lab workstation hardware/OS failure.")
        ));

        mockMvc.perform(post("/maintenance/reports")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"" + description + "\",\"floor\":\"Ground Floor\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.team").value("IT_SUPPORT"))
                .andExpect(jsonPath("$.aiCategory").value("IT_SUPPORT"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void scenario4_submitCleanlinessComplaint_classifiedAsHousekeeping() throws Exception {
        String description = "Trash bin is overflowing near table 12 and someone spilled drink on the carpet.";
        when(groqAIService.classify(contains("Trash bin"))).thenReturn(Optional.of(
                new AIClassificationResult(MaintenanceTeam.HOUSEKEEPING, MaintenanceTicket.Priority.LOW,
                        "Sanitation and spill cleaning required in study zone.")
        ));

        mockMvc.perform(post("/maintenance/reports")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"" + description + "\",\"floor\":\"Third Floor\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.team").value("HOUSEKEEPING"))
                .andExpect(jsonPath("$.aiCategory").value("HOUSEKEEPING"))
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void scenario5_cleanlinessFeedback_createsAndLinksHousekeepingTicketInMongo() throws Exception {
        String comment = "Restroom on the second floor has water leakage and dirty wash basins.";
        when(groqAIService.classify(contains("Restroom"))).thenReturn(Optional.of(
                new AIClassificationResult(MaintenanceTeam.HOUSEKEEPING, MaintenanceTicket.Priority.MEDIUM,
                        "Restroom maintenance and sanitization required.")
        ));

        MvcResult feedbackResult = mockMvc.perform(post("/feedback")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"CLEANLINESS\",\"rating\":2,\"comment\":\"" + comment + "\",\"floor\":\"Second Floor\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maintenanceTicketId").isNotEmpty())
                .andExpect(jsonPath("$.maintenanceCategory").value("HOUSEKEEPING"))
                .andExpect(jsonPath("$.maintenancePriority").value("MEDIUM"))
                .andExpect(jsonPath("$.maintenanceStatus").value("OPEN"))
                .andReturn();

        String ticketId = objectMapper.readTree(feedbackResult.getResponse().getContentAsString())
                .get("maintenanceTicketId").asText();
        MaintenanceTicket ticket = ticketRepository.findById(ticketId).orElseThrow();
        assertEquals(MaintenanceTeam.HOUSEKEEPING, ticket.getTeam());
        assertEquals("HOUSEKEEPING", ticket.getAiCategory());
        assertEquals(MaintenanceTicket.Priority.MEDIUM, ticket.getPriority());
        assertEquals(MaintenanceTicket.Status.OPEN, ticket.getStatus());
        assertEquals(studentId, ticket.getReportedBy());
        assertEquals("Second Floor", ticket.getFloor());
    }

    @Test
    void scenario6_maintenanceAdminCanViewTicketsAcrossAllTeams() throws Exception {
        when(groqAIService.classify(contains("router"))).thenReturn(Optional.of(
                new AIClassificationResult(MaintenanceTeam.NETWORK, MaintenanceTicket.Priority.HIGH, "Network issue")
        ));
        when(groqAIService.classify(contains("socket"))).thenReturn(Optional.of(
                new AIClassificationResult(MaintenanceTeam.ELECTRICAL, MaintenanceTicket.Priority.MEDIUM, "Electrical issue")
        ));

        mockMvc.perform(post("/maintenance/reports")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"The router is down.\",\"floor\":\"First Floor\"}")).andExpect(status().isCreated());

        mockMvc.perform(post("/maintenance/reports")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"Power socket sparked.\",\"floor\":\"Second Floor\"}")).andExpect(status().isCreated());

        mockMvc.perform(get("/maintenance/management/tickets")
                        .header("Authorization", "Bearer " + maintenanceAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void scenario7_networkStaffCannotAccessElectricalTickets() throws Exception {
        when(groqAIService.classify(contains("wire"))).thenReturn(Optional.of(
                new AIClassificationResult(MaintenanceTeam.ELECTRICAL, MaintenanceTicket.Priority.HIGH, "Electrical hazard")
        ));

        MvcResult result = mockMvc.perform(post("/maintenance/reports")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Exposed live wire behind desk.\",\"floor\":\"Floor 1\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String electricalTicketId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // Electrical staff CAN access
        mockMvc.perform(get("/maintenance/tickets/" + electricalTicketId)
                        .header("Authorization", "Bearer " + electricalStaffToken))
                .andExpect(status().isOk());

        // Network staff CANNOT access Electrical ticket
        mockMvc.perform(get("/maintenance/tickets/" + electricalTicketId)
                        .header("Authorization", "Bearer " + networkStaffToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void scenario8_unauthorizedUsersCannotModifyOrAssignTickets() throws Exception {
        when(groqAIService.classify(contains("cable"))).thenReturn(Optional.of(
                new AIClassificationResult(MaintenanceTeam.NETWORK, MaintenanceTicket.Priority.LOW, "Cable check")
        ));

        MvcResult result = mockMvc.perform(post("/maintenance/reports")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Loose network cable near chair.\",\"floor\":\"Floor 2\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String ticketId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // Student cannot reassign team
        mockMvc.perform(put("/maintenance/tickets/" + ticketId + "/team")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"team\":\"ELECTRICAL\"}"))
                .andExpect(status().isForbidden());

        // Student cannot update status
        mockMvc.perform(put("/maintenance/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isForbidden());

        // Normal Admin cannot access maintenance management tickets
        mockMvc.perform(get("/maintenance/management/tickets")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void scenario9_simulateGroqApiFailure_safePendingClassificationFallback() throws Exception {
        String description = "Unknown error with library facilities on third floor.";
        // Simulate Groq AI returning empty / failing
        when(groqAIService.classify(contains("Unknown error"))).thenReturn(Optional.empty());

        MvcResult result = mockMvc.perform(post("/maintenance/reports")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"" + description + "\",\"floor\":\"Third Floor\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_CLASSIFICATION"))
                .andExpect(jsonPath("$.team").doesNotExist())
                .andExpect(jsonPath("$.assignedStaffId").doesNotExist())
                .andExpect(jsonPath("$.classificationReason").isNotEmpty())
                .andReturn();

        String ticketId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
        MaintenanceTicket saved = ticketRepository.findById(ticketId).orElseThrow();
        assertEquals(MaintenanceTicket.Status.PENDING_CLASSIFICATION, saved.getStatus());
        assertNull(saved.getTeam());

        // Maintenance Admin can review and assign team manually
        mockMvc.perform(put("/maintenance/tickets/" + ticketId + "/team")
                        .header("Authorization", "Bearer " + maintenanceAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"team\":\"NETWORK\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.team").value("NETWORK"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignedStaffId").value(networkStaffId));
    }

    @Test
    void scenario10_studentCanViewTheirOwnSubmittedTicket() throws Exception {
        when(groqAIService.classify(contains("hotspot"))).thenReturn(Optional.of(
                new AIClassificationResult(MaintenanceTeam.NETWORK, MaintenanceTicket.Priority.MEDIUM, "Hotspot issue")
        ));

        MvcResult result = mockMvc.perform(post("/maintenance/reports")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Library Wi-Fi hotspot disconnection issue.\",\"floor\":\"Ground Floor\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String ticketId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // The student who reported it CAN view it
        mockMvc.perform(get("/maintenance/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId));

        // Another student CANNOT view it
        mockMvc.perform(get("/maintenance/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + otherStudentToken))
                .andExpect(status().isForbidden());
    }

    private void saveUser(String id, String role, MaintenanceTeam team) {
        User user = new User();
        user.setStudentId(id);
        user.setName(id);
        user.setEmail(id + "@example.com");
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setMaintenanceTeam(team);
        user.setActive(true);
        userRepository.save(user);
    }

    private String login(String id) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"" + id + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
