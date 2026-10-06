package library_management.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import library_management.model.FloorCapacity;

public interface FloorCapacityRepository extends MongoRepository<FloorCapacity, String> {
}
