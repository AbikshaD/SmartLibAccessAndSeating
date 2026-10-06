package library_management;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import library_management.model.Seat;
import library_management.model.User;
import library_management.repository.BookingRepository;
import library_management.repository.SeatRepository;
import library_management.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class BookingManagementTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private BookingRepository bookingRepository;

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
    private Seat testSeat;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        adminId = "booking-admin-" + suffix;
        studentId = "booking-student-" + suffix;
        otherStudentId = "booking-other-" + suffix;
        saveUser(adminId, "ADMIN");
        saveUser(studentId, "STUDENT");
        saveUser(otherStudentId, "STUDENT");
        adminToken = login(adminId);
        studentToken = login(studentId);
        otherStudentToken = login(otherStudentId);

        testSeat = new Seat();
        testSeat.setSeatNumber("B-" + suffix);
        testSeat.setFloor("Ground");
        testSeat.setStatus(Seat.Status.AVAILABLE);
        testSeat = seatRepository.save(testSeat);
    }

    @AfterEach
    void cleanUp() {
        if (testSeat != null && testSeat.getId() != null) {
            bookingRepository.findAll().stream()
                    .filter(booking -> booking.getSeatId().equals(testSeat.getSeatNumber()))
                    .forEach(bookingRepository::delete);
            seatRepository.deleteById(testSeat.getId());
        }
        userRepository.findByStudentId(adminId).ifPresent(userRepository::delete);
        userRepository.findByStudentId(studentId).ifPresent(userRepository::delete);
        userRepository.findByStudentId(otherStudentId).ifPresent(userRepository::delete);
    }

    @Test
    void studentCanBookCancelAndOnlyReadTheirOwnBookings() throws Exception {
        MvcResult result = mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(testSeat.getSeatNumber(), otherStudentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value(studentId))
                .andExpect(jsonPath("$.seatId").value(testSeat.getSeatNumber()))
                .andExpect(jsonPath("$.seatDocumentId").doesNotExist())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andReturn();
        String bookingId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        org.junit.jupiter.api.Assertions.assertEquals(Seat.Status.BOOKED,
                seatRepository.findById(testSeat.getId()).orElseThrow().getStatus());
        mockMvc.perform(get("/seats/available/ground").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + testSeat.getId() + "')]").isEmpty());

        mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + otherStudentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(testSeat.getSeatNumber(), otherStudentId)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/bookings/" + bookingId)
                        .header("Authorization", "Bearer " + otherStudentToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/bookings/" + bookingId)
                        .header("Authorization", "Bearer " + otherStudentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/bookings/my").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(bookingId));

        mockMvc.perform(delete("/bookings/" + bookingId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        org.junit.jupiter.api.Assertions.assertEquals(Seat.Status.AVAILABLE,
                seatRepository.findById(testSeat.getId()).orElseThrow().getStatus());
        mockMvc.perform(get("/bookings").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/bookings").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        MvcResult secondBooking = mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(testSeat.getSeatNumber(), studentId)))
                .andExpect(status().isCreated())
                .andReturn();
        String secondBookingId = objectMapper.readTree(secondBooking.getResponse().getContentAsString())
                .get("id").asText();
        mockMvc.perform(delete("/bookings/" + secondBookingId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void adminCanGenerateSeatsAndDuplicateRangesAreRejected() throws Exception {
        mockMvc.perform(post("/seats/generate")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"floor\":\"THIRD\",\"numberOfSeats\":2}"))
                .andExpect(status().isForbidden());

        MvcResult result = mockMvc.perform(post("/seats/generate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"floor\":\"GROUND\",\"numberOfSeats\":50}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].seatNumber").value("G-01"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[49].seatNumber").value("G-50"))
                .andReturn();
        var generated = objectMapper.readTree(result.getResponse().getContentAsString());
        generated.forEach(seat -> seatRepository.deleteById(seat.get("id").asText()));

        mockMvc.perform(post("/seats/generate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"floor\":\"GROUND\",\"numberOfSeats\":50}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/seats/generate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"floor\":\"GROUND\",\"numberOfSeats\":50}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/seats/generate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"floor\":\"ROOFTOP\",\"numberOfSeats\":2}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/seats/generate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"floor\":\"GROUND\",\"numberOfSeats\":0}"))
                .andExpect(status().isBadRequest());
        seatRepository.findByFloor("GROUND").stream()
                .filter(seat -> seat.getSeatNumber().matches("G-(0[1-9]|[1-4][0-9]|50)"))
                .forEach(seatRepository::delete);

        mockMvc.perform(get("/seats/floor/ground").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/seats/floor/ROOFTOP").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest());
    }

    private void saveUser(String id, String role) {
        User user = new User();
        user.setStudentId(id);
        user.setName(id);
        user.setEmail(id + "@example.com");
        user.setPassword(passwordEncoder.encode("booking-test-password"));
        user.setRole(role);
        userRepository.save(user);
    }

    private String login(String id) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"" + id + "\",\"password\":\"booking-test-password\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return response.replaceFirst(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }

    private String bookingJson(String seatId, String claimedStudentId) {
        return "{\"seatId\":\"" + seatId + "\",\"studentId\":\"" + claimedStudentId
                + "\",\"bookingDate\":\"" + LocalDate.now().plusDays(1)
                + "\",\"startTime\":\"09:00\",\"endTime\":\"10:00\"}";
    }
}