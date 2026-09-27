package library_management.service;

import java.util.List;

import org.springframework.stereotype.Service;

import library_management.exception.SeatNotFoundException;
import library_management.exception.SeatOccupiedException;
import library_management.model.Seat;
import library_management.repository.SeatRepository;

@Service
public class SeatService {

    private final SeatRepository seatRepository;

    public SeatService(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    public Seat createSeat(Seat seat) {
        if (seat.getStatus() == null) {
            seat.setStatus(Seat.Status.AVAILABLE);
        }
        return seatRepository.save(seat);
    }

    public List<Seat> getAllSeats() {
        return seatRepository.findAll();
    }

    public List<Seat> getAvailableSeats() {
        return seatRepository.findAll().stream()
                .filter(seat -> seat.getStatus() == Seat.Status.AVAILABLE)
                .toList();
    }

    public Seat getSeatById(String id) {
        return SeatRepository.findBySeatId(id);
    }

    public Seat updateSeat(String id, Seat updatedSeat) {
        Seat existingSeat = findSeat(id);
        existingSeat.setSeatNumber(updatedSeat.getSeatNumber());
        existingSeat.setFloor(updatedSeat.getFloor());
        existingSeat.setStatus(updatedSeat.getStatus());
        return seatRepository.save(existingSeat);
    }

    public void deleteSeat(String id) {
        Seat seat = findSeat(id);
        if (seat.getStatus() == Seat.Status.OCCUPIED) {
            throw new SeatOccupiedException(seat.getSeatNumber());
        }
        seatRepository.delete(seat);
    }

    private Seat findSeat(String id) {
        return seatRepository.findById(id).orElseThrow(() -> new SeatNotFoundException(id));
    }
}