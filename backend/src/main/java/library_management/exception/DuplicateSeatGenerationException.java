package library_management.exception;

public class DuplicateSeatGenerationException extends RuntimeException {
    public DuplicateSeatGenerationException(String floor, String seatNumber) {
        super("Seat already exists for floor " + floor + ": " + seatNumber);
    }
}