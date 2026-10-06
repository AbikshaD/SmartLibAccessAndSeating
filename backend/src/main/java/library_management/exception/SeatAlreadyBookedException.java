package library_management.exception;

public class SeatAlreadyBookedException extends RuntimeException {
    public SeatAlreadyBookedException(String seatNumber) {
        super("Seat is already booked: " + seatNumber);
    }
}