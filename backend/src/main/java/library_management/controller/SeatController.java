package library_management.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.annotation.Validated;

import library_management.model.Seat;
import library_management.model.FloorCapacity;
import library_management.service.SeatService;

@RestController
@Validated
@RequestMapping("/seats")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Seat>> generateSeats(@Valid @RequestBody GenerateSeatsRequest request) {
        List<Seat> generatedSeats = seatService.generateSeats(request.floor(), request.numberOfSeats());
        return ResponseEntity.status(201).body(generatedSeats);
    }

    @PutMapping("/capacity")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeatService.FloorCapacityResult> setFloorCapacity(
            @Valid @RequestBody SetFloorCapacityRequest request) {
        return ResponseEntity.ok(seatService.setFloorCapacity(request.floor(), request.maxSeats()));
    }

    @GetMapping("/capacity")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FloorCapacity>> getFloorCapacities() {
        return ResponseEntity.ok(seatService.getFloorCapacities());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Seat> createSeat(@Valid @RequestBody Seat seat) {
        Seat createdSeat = seatService.createSeat(seat);
        return ResponseEntity.created(URI.create("/seats/" + createdSeat.getId())).body(createdSeat);
    }

    @GetMapping
    public ResponseEntity<List<Seat>> getAllSeats() {
        return ResponseEntity.ok(seatService.getAllSeats());
    }

    @GetMapping("/floor/{floor}")
    public ResponseEntity<List<Seat>> getSeatsByFloor(@PathVariable @NotBlank String floor) {
        return ResponseEntity.ok(seatService.getSeatsByFloor(floor));
    }

    @GetMapping("/available")
    public ResponseEntity<List<Seat>> getAvailableSeats() {
        return ResponseEntity.ok(seatService.getAvailableSeats());
    }

    @GetMapping("/available/{floor}")
    public ResponseEntity<List<Seat>> getAvailableSeatsByFloor(@PathVariable @NotBlank String floor) {
        return ResponseEntity.ok(seatService.getAvailableSeatsByFloor(floor));
    }

    @GetMapping("/{seatId}")
    public ResponseEntity<Seat> getSeatById(@PathVariable String seatId) {
        return ResponseEntity.ok(seatService.getSeatById(seatId));
    }

    @PutMapping("/{seatId}")
    public ResponseEntity<Seat> updateSeat(@PathVariable String seatId, @Valid @RequestBody Seat seat) {
        return ResponseEntity.ok(seatService.updateSeat(seatId, seat));
    }

    @DeleteMapping("/{seatId}")
    public ResponseEntity<Void> deleteSeat(@PathVariable String seatId) {
        seatService.deleteSeat(seatId);
        return ResponseEntity.noContent().build();
    }

    public record GenerateSeatsRequest(
            @NotBlank String floor,
            @Min(1) @Max(500) int numberOfSeats) { }

    public record SetFloorCapacityRequest(
            @NotBlank String floor,
            @Min(1) @Max(500) int maxSeats) { }
}