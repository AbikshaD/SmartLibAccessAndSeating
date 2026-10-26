package library_management;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import library_management.model.User;
import library_management.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationAuthorizationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String studentId;
    private String adminId;
    private String maintenanceAdminId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        studentId = "student-" + suffix;
        adminId = "admin-" + suffix;
        maintenanceAdminId = "maintenance-admin-" + suffix;
    }

    @AfterEach
    void cleanUp() {
        userRepository.findByStudentId(studentId).ifPresent(userRepository::delete);
        userRepository.findByStudentId(adminId).ifPresent(userRepository::delete);
        userRepository.findByStudentId(maintenanceAdminId).ifPresent(userRepository::delete);
    }

    @Test
    void registrationHashesPasswordAndDoesNotReturnIt() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(studentId, "student@example.com", "student-pass", "ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist());

        User savedUser = userRepository.findByStudentId(studentId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("student-pass", savedUser.getPassword()));
        org.junit.jupiter.api.Assertions.assertEquals("STUDENT", savedUser.getRole());
    }

    @Test
    void invalidCredentialsAreRejected() throws Exception {
        registerStudent();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(studentId, "wrong-pass")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void corsPreflightFromLocalFrontendIsAllowed() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mockMvc.perform(options("/auth/login")
                .header("Origin", "https://friend-frontend.ngrok-free.dev")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,authorization"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin",
                "https://friend-frontend.ngrok-free.dev"));
    }

    @Test
    void studentCannotUseProtectedEndpointsWithoutOrWithInsufficientRole() throws Exception {
        registerStudent();
        String token = login(studentId, "student-pass");

        mockMvc.perform(get("/users/" + studentId)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/users/" + studentId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanRegisterAdminAndAccessAdministrativeEndpoint() throws Exception {
        registerStudent();
        String studentToken = login(studentId, "student-pass");

        mockMvc.perform(post("/users/admin")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(adminId, "admin@example.com", "admin-pass", "STUDENT")))
                .andExpect(status().isForbidden());

        User admin = new User();
        admin.setStudentId(adminId);
        admin.setName("Admin");
        admin.setEmail(adminId + "@example.com");
        admin.setPassword(passwordEncoder.encode("admin-pass"));
        admin.setRole("ADMIN");
        userRepository.save(admin);

        String adminToken = login(adminId, "admin-pass");
        mockMvc.perform(post("/users/admin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("second-" + adminId, "second-" + adminId + "@example.com", "admin-pass", "STUDENT")))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        userRepository.findByStudentId("second-" + adminId).ifPresent(userRepository::delete);

        mockMvc.perform(post("/users/maintenance-admin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(maintenanceAdminId, maintenanceAdminId + "@example.com",
                                "maintenance-admin-pass", "STUDENT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MAINTENANCE_ADMIN"));

        String maintenanceAdminToken = login(maintenanceAdminId, "maintenance-admin-pass");
        mockMvc.perform(get("/users").header("Authorization", "Bearer " + maintenanceAdminToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/users/admin")
                        .header("Authorization", "Bearer " + maintenanceAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("forbidden-" + adminId, "forbidden-" + adminId + "@example.com",
                                "maintenance-admin-pass", "ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/maintenance/management/tickets")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/maintenance/management/tickets")
                        .header("Authorization", "Bearer " + maintenanceAdminToken))
                .andExpect(status().isOk());
        mockMvc.perform(put("/users/" + maintenanceAdminId + "/active")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(maintenanceAdminId, "maintenance-admin-pass")))
                .andExpect(status().isUnauthorized());
    }

    private void registerStudent() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(studentId, studentId + "@example.com", "student-pass", "STUDENT")))
                .andExpect(status().isCreated());
    }

    private String login(String id, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(id, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return response.replaceFirst(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }

    private String userJson(String id, String email, String password, String role) {
        return "{\"studentId\":\"" + id + "\",\"name\":\"Test User\",\"email\":\""
                + email + "\",\"password\":\"" + password + "\",\"role\":\"" + role + "\"}";
    }

    private String loginJson(String id, String password) {
        return "{\"studentId\":\"" + id + "\",\"password\":\"" + password + "\"}";
    }
}