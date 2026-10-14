package library_management.exception;

public class BookingTimeConflictException extends RuntimeException {
    public BookingTimeConflictException() {
        super("You already have a confirmed booking that overlaps this time.");
    }
}
