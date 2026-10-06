package library_management.exception;

public class InvalidFloorException extends RuntimeException {
    public InvalidFloorException(String floor) {
        super("Invalid floor: " + floor + ". Supported floors are GROUND, FIRST, SECOND, and THIRD.");
    }
}