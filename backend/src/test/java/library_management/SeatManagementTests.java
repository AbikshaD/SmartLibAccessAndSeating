package library_management;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.contains;

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
import org.springframework.test.web.servlet.MvcResult;

import library_management.model.Seat;
import library_management.repository.FloorCapacityRepository;
import library_management.model.User;
import library_management.repository.SeatRepository;
import library_management.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class SeatManagementTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private FloorCapacityRepository floorCapacityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminId;
    private String studentId;
    private String adminToken;
    private String studentToken;
    private String seatNumber;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        adminId = "seat-admin-" + suffix;
        studentId = "seat-student-" + suffix;
        seatNumber = "G-" + suffix;
        saveUser(adminId, "ADMIN");
        saveUser(studentId, "STUDENT");
        adminToken = login(adminId);
        studentToken = login(studentId);
    }

    @AfterEach
    void cleanUp() {
        seatRepository.findAll().stream()
                .filter(seat -> seat.getSeatNumber().equals(seatNumber)
                        || seat.getSeatNumber().equals("UPDATED-" + seatNumber)
                        || (seat.getFloor().equals("SECOND")
                                && (seat.getSeatNumber().equals("S-01") || seat.getSeatNumber().equals("S-02"))))
                .forEach(seatRepository::delete);
        floorCapacityRepository.deleteById("SECOND");
        userRepository.findByStudentId(adminId).ifPresent(userRepository::delete);
        userRepository.findByStudentId(studentId).ifPresent(userRepository::delete);
    }

    @Test
    void adminCanCreateReadUpdateAndDeleteSeatsWhileStudentsCanOnlyRead() throws Exception {
        mockMvc.perform(post("/seats")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(seatJson(seatNumber, "Ground", "AVAILABLE")))
                .andExpect(status().isForbidden());

        MvcResult creation = mockMvc.perform(post("/seats")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(seatJson(seatNumber, "Ground", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andReturn();
        String seatId = creation.getResponse().getHeader("Location").replace("/seats/", "");

        mockMvc.perform(get("/seats").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.seatNumber=='" + seatNumber + "')]").isNotEmpty());
        mockMvc.perform(get("/seats/available").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.seatNumber=='" + seatNumber + "')].status").value("AVAILABLE"));
        mockMvc.perform(get("/seats/" + seatId).header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.floor").value("Ground"));

        mockMvc.perform(put("/seats/" + seatId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(seatJson("UPDATED-" + seatNumber, "First", "AVAILABLE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatNumber").value("UPDATED-" + seatNumber))
                .andExpect(jsonPath("$.floor").value("First"));
        mockMvc.perform(get("/seats/" + seatId).header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.floor").value("First"));

        mockMvc.perform(delete("/seats/" + seatId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/seats/" + seatId).header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/seats")).andExpect(status().isUnauthorized());
    }

    @Test
    void occupiedSeatsCannotBeDeleted() throws Exception {
        Seat occupiedSeat = new Seat();
        occupiedSeat.setSeatNumber(seatNumber);
        occupiedSeat.setFloor("Ground");
        occupiedSeat.setStatus(Seat.Status.OCCUPIED);
        Seat savedSeat = seatRepository.save(occupiedSeat);

        mockMvc.perform(delete("/seats/" + savedSeat.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }

    @Test
    void adminCanSetFloorCapacityAndSeatsAreCreatedUpToThatLimit() throws Exception {
        mockMvc.perform(put("/seats/capacity")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"floor\":\"SECOND\",\"maxSeats\":2}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/seats/capacity")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"floor\":\"SECOND\",\"maxSeats\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.floor").value("SECOND"))
                .andExpect(jsonPath("$.maxSeats").value(2))
                .andExpect(jsonPath("$.totalSeats").value(2))
                .andExpect(jsonPath("$.createdSeats.length()").value(2))
                .andExpect(jsonPath("$.createdSeats[0].seatNumber").value("S-01"));

        mockMvc.perform(put("/seats/capacity")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"floor\":\"SECOND\",\"maxSeats\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSeats").value(2))
                .andExpect(jsonPath("$.createdSeats.length()").value(0));

        mockMvc.perform(put("/seats/capacity")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"floor\":\"SECOND\",\"maxSeats\":1}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/seats/capacity").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.floor=='SECOND')].maxSeats").value(contains(2)));
    }

    @Test
    void floorCapacityRejectsUnsupportedFloorAndInvalidMaximum() throws Exception {
        mockMvc.perform(put("/seats/capacity")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"floor\":\"ROOFTOP\",\"maxSeats\":2}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/seats/capacity")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"floor\":\"FIRST\",\"maxSeats\":501}"))
                .andExpect(status().isBadRequest());
    }

    private void saveUser(String id, String role) {
        User user = new User();
        user.setStudentId(id);
        user.setName(id);
        user.setEmail(id + "@example.com");
        user.setPassword(passwordEncoder.encode("seat-test-password"));
        user.setRole(role);
        userRepository.save(user);
    }

    private String login(String id) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"" + id + "\",\"password\":\"seat-test-password\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return response.replaceFirst(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }

    private String seatJson(String number, String floor, String status) {
        String statusField = status == null ? "" : ",\"status\":\"" + status + "\"";
        return "{\"seatNumber\":\"" + number + "\",\"floor\":\"" + floor + "\"" + statusField + "}";
    }
}