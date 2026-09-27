package library_management.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import library_management.model.Seat;

public interface SeatRepository extends MongoRepository<Seat, String> {

    static Seat findBySeatId(String id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findBySeatId'");
    }
}