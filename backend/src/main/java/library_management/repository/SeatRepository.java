package library_management.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import library_management.model.Seat;

public interface SeatRepository extends MongoRepository<Seat, String> {
}