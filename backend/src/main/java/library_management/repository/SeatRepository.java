package library_management.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

import library_management.model.Seat;

public interface SeatRepository extends MongoRepository<Seat, String> {
	List<Seat> findByFloor(String floor);

	List<Seat> findByFloorIgnoreCase(String floor);

	List<Seat> findByFloorAndStatus(String floor, Seat.Status status);

	List<Seat> findByFloorIgnoreCaseAndStatus(String floor, Seat.Status status);

	List<Seat> findByStatus(Seat.Status status);

	List<Seat> findBySeatNumber(String seatNumber);

	boolean existsByFloorAndSeatNumber(String floor, String seatNumber);
}