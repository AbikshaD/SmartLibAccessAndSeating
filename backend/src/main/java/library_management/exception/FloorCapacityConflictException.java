package library_management.exception;

public class FloorCapacityConflictException extends RuntimeException {
    public FloorCapacityConflictException(String floor, int currentSeats, int requestedCapacity) {
        super("Floor " + floor + " already has " + currentSeats
                + " seats; capacity cannot be reduced to " + requestedCapacity + ".");
    }
}
