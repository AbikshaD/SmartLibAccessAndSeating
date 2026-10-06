package library_management.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import library_management.model.Booking;
import library_management.service.BookingService;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Booking> createBooking(@Valid @RequestBody CreateBookingRequest request,
            Authentication authentication) {
        Booking booking = bookingService.createBooking(authentication.getName(),
                new BookingService.CreateBookingRequest(request.seatId(), request.bookingDate(),
                        request.startTime(), request.endTime()));
        return ResponseEntity.created(URI.create("/bookings/" + booking.getId())).body(booking);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<List<Booking>> getMyBookings(Authentication authentication) {
        return ResponseEntity.ok(bookingService.getMyBookings(authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Booking>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @GetMapping("/{bookingId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<Booking> getBooking(@PathVariable String bookingId, Authentication authentication) {
        return ResponseEntity.ok(bookingService.getBooking(bookingId, authentication.getName(), isAdmin(authentication)));
    }

    @DeleteMapping("/{bookingId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<Booking> cancelBooking(@PathVariable String bookingId, Authentication authentication) {
        Booking booking = bookingService.cancelBooking(bookingId, authentication.getName(), isAdmin(authentication));
        return ResponseEntity.ok(booking);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    public record CreateBookingRequest(
            @NotBlank String seatId,
            @NotNull @FutureOrPresent LocalDate bookingDate,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime) { }
}