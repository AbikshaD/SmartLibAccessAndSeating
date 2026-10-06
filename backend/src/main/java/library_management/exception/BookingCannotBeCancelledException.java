package library_management.exception;

public class BookingCannotBeCancelledException extends RuntimeException {
    public BookingCannotBeCancelledException(String bookingId) {
        super("Booking cannot be cancelled: " + bookingId);
    }
}