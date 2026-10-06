package library_management.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import library_management.model.Booking;

public interface BookingRepository extends MongoRepository<Booking, String> {
    List<Booking> findByStudentIdOrderByBookingDateDesc(String studentId);

    Optional<Booking> findByIdAndStudentId(String id, String studentId);
}