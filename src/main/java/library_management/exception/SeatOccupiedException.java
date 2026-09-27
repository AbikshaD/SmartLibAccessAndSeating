package library_management.exception;

public class SeatOccupiedException extends RuntimeException {

    public SeatOccupiedException(String seatNumber) {
        super("Cannot delete occupied seat: " + seatNumber);
    }
}