package library_management;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

import library_management.model.Booking;
import library_management.model.Seat;
import library_management.model.User;
import library_management.repository.BookingRepository;
import library_management.repository.SeatRepository;
import library_management.repository.UserRepository;
import library_management.service.BookingService;

@SpringBootTest
@AutoConfigureMockMvc
class BookingManagementTests {

        private static final String TEST_DATABASE = "booking_tests_"
                        + UUID.randomUUID().toString().replace("-", "");

        @DynamicPropertySource
        static void configureMongoDatabase(DynamicPropertyRegistry registry) {
                registry.add("spring.mongodb.database", () -> TEST_DATABASE);
        }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingService bookingService;

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
        org.junit.jupiter.api.Assertions.assertEquals(1,
                userRepository.findByStudentId(studentId).orElseThrow().getBookingCancellationCount());
    }

    @Test
    void fourthStudentCancellationBlocksBookingUntilAdminEnablesIt() throws Exception {
        for (int cancellation = 1; cancellation <= 4; cancellation++) {
            MvcResult created = mockMvc.perform(post("/bookings")
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bookingJson(testSeat.getSeatNumber(), studentId)))
                    .andExpect(status().isCreated())
                    .andReturn();
            String bookingId = objectMapper.readTree(created.getResponse().getContentAsString())
                    .get("id").asText();

            mockMvc.perform(delete("/bookings/" + bookingId)
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk());
        }

        User blockedStudent = userRepository.findByStudentId(studentId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(blockedStudent.isBlocked());
        org.junit.jupiter.api.Assertions.assertEquals(4, blockedStudent.getBookingCancellationCount());
        org.junit.jupiter.api.Assertions.assertEquals(Seat.Status.AVAILABLE,
                seatRepository.findById(testSeat.getId()).orElseThrow().getStatus());

        mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(testSeat.getSeatNumber(), studentId)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/users/" + studentId + "/unblock")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/users/" + studentId + "/unblock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(false))
                .andExpect(jsonPath("$.bookingCancellationCount").value(0));

        mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(testSeat.getSeatNumber(), studentId)))
                .andExpect(status().isCreated());
    }

    @Test
    void expiredBookingCompletesAndReleasesItsSeat() {
        testSeat.setStatus(Seat.Status.BOOKED);
        seatRepository.save(testSeat);

        Booking booking = new Booking();
        booking.setStudentId(studentId);
        booking.setSeatId(testSeat.getSeatNumber());
        booking.setSeatDocumentId(testSeat.getId());
        booking.setSeatNumber(testSeat.getSeatNumber());
        booking.setFloor(testSeat.getFloor());
        booking.setBookingDate(LocalDate.now());
        booking.setStartTime(LocalTime.now().minusHours(1));
        booking.setEndTime(LocalTime.now().minusMinutes(1));
        booking.setStatus(Booking.Status.CONFIRMED);
        booking = bookingRepository.save(booking);

        org.junit.jupiter.api.Assertions.assertEquals(1, bookingService.getMyBookings(studentId).size());
        org.junit.jupiter.api.Assertions.assertEquals(Booking.Status.COMPLETED,
                bookingRepository.findById(booking.getId()).orElseThrow().getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(Seat.Status.AVAILABLE,
                seatRepository.findById(testSeat.getId()).orElseThrow().getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(0, bookingService.completeExpiredBookings());
    }

    @Test
    void bookingCannotStartEarlierThanCurrentTimeOnTheSameDay() {
        org.junit.jupiter.api.Assumptions.assumeTrue(LocalDateTime.now().isAfter(LocalDate.now().atStartOfDay()));
        BookingService.CreateBookingRequest request = new BookingService.CreateBookingRequest(
                testSeat.getSeatNumber(), LocalDate.now(), LocalTime.MIDNIGHT, LocalTime.of(23, 59));

        org.junit.jupiter.api.Assertions.assertThrows(
                library_management.exception.InvalidBookingRequestException.class,
                () -> bookingService.createBooking(studentId, request));
        org.junit.jupiter.api.Assertions.assertEquals(Seat.Status.AVAILABLE,
                seatRepository.findById(testSeat.getId()).orElseThrow().getStatus());
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

    @Test
    void studentBlockedAfterMoreThan3CancellationsAndAdminCanUnblock() throws Exception {
        for (int i = 1; i <= 4; i++) {
            MvcResult result = mockMvc.perform(post("/bookings")
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bookingJson(testSeat.getSeatNumber(), studentId)))
                    .andExpect(status().isCreated())
                    .andReturn();
            String bookingId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
            mockMvc.perform(delete("/bookings/" + bookingId)
                            .header("Authorization", "Bearer " + studentToken))
                    .andExpect(status().isOk());
        }

        User student = userRepository.findByStudentId(studentId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(student.isBlocked());

        mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(testSeat.getSeatNumber(), studentId)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/users/" + studentId + "/unblock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        User unblockedStudent = userRepository.findByStudentId(studentId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertFalse(unblockedStudent.isBlocked());

        MvcResult finalResult = mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(testSeat.getSeatNumber(), studentId)))
                .andExpect(status().isCreated())
                .andReturn();
        String finalBookingId = objectMapper.readTree(finalResult.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(delete("/bookings/" + finalBookingId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());
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