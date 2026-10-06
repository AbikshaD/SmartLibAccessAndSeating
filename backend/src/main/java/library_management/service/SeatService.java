package library_management.service;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Service;

import library_management.exception.DuplicateSeatGenerationException;
import library_management.exception.FloorCapacityConflictException;
import library_management.exception.InvalidFloorException;
import library_management.exception.SeatNotFoundException;
import library_management.exception.SeatAlreadyBookedException;
import library_management.exception.SeatOccupiedException;
import library_management.model.FloorCapacity;
import library_management.model.Seat;
import library_management.repository.FloorCapacityRepository;
import library_management.repository.SeatRepository;

@Service
public class SeatService {

    private final SeatRepository seatRepository;
    private final FloorCapacityRepository floorCapacityRepository;
    private final MongoTemplate mongoTemplate;

    public SeatService(SeatRepository seatRepository, FloorCapacityRepository floorCapacityRepository,
            MongoTemplate mongoTemplate) {
        this.seatRepository = seatRepository;
        this.floorCapacityRepository = floorCapacityRepository;
        this.mongoTemplate = mongoTemplate;
        mongoTemplate.indexOps(Seat.class).createIndex(new Index()
                .on("floor", Sort.Direction.ASC)
                .on("seatNumber", Sort.Direction.ASC)
                .unique()
                .named("floor_seat_number_unique"));
    }

    public Seat createSeat(Seat seat) {
        if (seat.getStatus() == null) {
            seat.setStatus(Seat.Status.AVAILABLE);
        }
        if (seat.getStatus() == Seat.Status.BOOKED) {
            throw new SeatAlreadyBookedException(seat.getSeatNumber());
        }
        try {
            return seatRepository.save(seat);
        } catch (DuplicateKeyException exception) {
            throw new DuplicateSeatGenerationException(seat.getFloor(), seat.getSeatNumber());
        }
    }

    public List<Seat> generateSeats(String requestedFloor, int numberOfSeats) {
        String floor = normalizeFloor(requestedFloor);
        String prefix = floorPrefix(floor);
        List<Seat> generatedSeats = new ArrayList<>(numberOfSeats);

        for (int seatIndex = 1; seatIndex <= numberOfSeats; seatIndex++) {
            String seatNumber = prefix + "-%02d".formatted(seatIndex);
            if (seatRepository.existsByFloorAndSeatNumber(floor, seatNumber)) {
                throw new DuplicateSeatGenerationException(floor, seatNumber);
            }

            Seat seat = new Seat();
            seat.setFloor(floor);
            seat.setSeatNumber(seatNumber);
            seat.setStatus(Seat.Status.AVAILABLE);
            generatedSeats.add(seat);
        }

        try {
            return seatRepository.saveAll(generatedSeats);
        } catch (DuplicateKeyException exception) {
            throw new DuplicateSeatGenerationException(floor, "generated range");
        }
    }

    public FloorCapacityResult setFloorCapacity(String requestedFloor, int maxSeats) {
        String floor = normalizeFloor(requestedFloor);
        List<Seat> existingSeats = seatRepository.findByFloorIgnoreCase(floor);
        if (existingSeats.size() > maxSeats) {
            throw new FloorCapacityConflictException(floor, existingSeats.size(), maxSeats);
        }

        String prefix = floorPrefix(floor);
        List<Seat> generatedSeats = new ArrayList<>(maxSeats - existingSeats.size());
        Set<String> existingSeatNumbers = new HashSet<>();
        existingSeats.stream().map(Seat::getSeatNumber).forEach(existingSeatNumbers::add);
        for (int seatIndex = 1; seatIndex <= 500 && existingSeats.size() + generatedSeats.size() < maxSeats;
                seatIndex++) {
            String seatNumber = prefix + "-%02d".formatted(seatIndex);
            if (existingSeatNumbers.contains(seatNumber)) {
                continue;
            }

            Seat seat = new Seat();
            seat.setFloor(floor);
            seat.setSeatNumber(seatNumber);
            seat.setStatus(Seat.Status.AVAILABLE);
            generatedSeats.add(seat);
        }

        List<Seat> savedSeats;
        try {
            savedSeats = generatedSeats.isEmpty() ? List.of() : seatRepository.saveAll(generatedSeats);
        } catch (DuplicateKeyException exception) {
            throw new DuplicateSeatGenerationException(floor, "generated range");
        }

        FloorCapacity capacity = new FloorCapacity();
        capacity.setFloor(floor);
        capacity.setMaxSeats(maxSeats);
        floorCapacityRepository.save(capacity);

        return new FloorCapacityResult(floor, maxSeats, existingSeats.size() + savedSeats.size(), savedSeats);
    }

    public List<FloorCapacity> getFloorCapacities() {
        return floorCapacityRepository.findAll();
    }

    public List<Seat> getAllSeats() {
        return seatRepository.findAll();
    }

    public List<Seat> getAvailableSeats() {
        return seatRepository.findByStatus(Seat.Status.AVAILABLE);
    }

    public List<Seat> getSeatsByFloor(String requestedFloor) {
        return seatRepository.findByFloorIgnoreCase(normalizeFloor(requestedFloor));
    }

    public List<Seat> getAvailableSeatsByFloor(String requestedFloor) {
        return seatRepository.findByFloorIgnoreCaseAndStatus(
                normalizeFloor(requestedFloor), Seat.Status.AVAILABLE);
    }

    public Seat getSeatById(String id) {
        return findSeat(id);
    }

    public Seat updateSeat(String id, Seat updatedSeat) {
        Seat existingSeat = findSeat(id);
        if (existingSeat.getStatus() == Seat.Status.BOOKED || updatedSeat.getStatus() == Seat.Status.BOOKED) {
            throw new SeatAlreadyBookedException(existingSeat.getSeatNumber());
        }
        try {
            Seat updated = mongoTemplate.findAndModify(
                    org.springframework.data.mongodb.core.query.Query.query(
                            org.springframework.data.mongodb.core.query.Criteria.where("_id").is(id)
                                    .and("status").ne(Seat.Status.BOOKED)),
                    new org.springframework.data.mongodb.core.query.Update()
                            .set("seatNumber", updatedSeat.getSeatNumber())
                            .set("floor", updatedSeat.getFloor())
                            .set("status", updatedSeat.getStatus()),
                    org.springframework.data.mongodb.core.FindAndModifyOptions.options().returnNew(true),
                    Seat.class);
            if (updated == null) {
                if (!seatRepository.existsById(id)) {
                    throw new SeatNotFoundException(id);
                }
                throw new SeatAlreadyBookedException(existingSeat.getSeatNumber());
            }
            return updated;
        } catch (DuplicateKeyException exception) {
            throw new DuplicateSeatGenerationException(updatedSeat.getFloor(), updatedSeat.getSeatNumber());
        }
    }

    public void deleteSeat(String id) {
        Seat seat = findSeat(id);
        Seat deleted = mongoTemplate.findAndRemove(
                org.springframework.data.mongodb.core.query.Query.query(
                        org.springframework.data.mongodb.core.query.Criteria.where("_id").is(id)
                                .and("status").is(Seat.Status.AVAILABLE)),
                Seat.class);
        if (deleted == null) {
            if (!seatRepository.existsById(id)) {
                throw new SeatNotFoundException(id);
            }
            throw new SeatOccupiedException(seat.getSeatNumber());
        }
    }

    private Seat findSeat(String id) {
        return seatRepository.findById(id).orElseThrow(() -> new SeatNotFoundException(id));
    }

    private String normalizeFloor(String requestedFloor) {
        if (requestedFloor == null || requestedFloor.isBlank()) {
            throw new InvalidFloorException(requestedFloor);
        }
        String floor = requestedFloor.trim().toUpperCase(Locale.ROOT);
        floorPrefix(floor);
        return floor;
    }

    private String floorPrefix(String floor) {
        return switch (floor) {
            case "GROUND" -> "G";
            case "FIRST" -> "F";
            case "SECOND" -> "S";
            case "THIRD" -> "T";
            default -> throw new InvalidFloorException(floor);
        };
    }

    public record FloorCapacityResult(String floor, int maxSeats, int totalSeats, List<Seat> createdSeats) { }
}