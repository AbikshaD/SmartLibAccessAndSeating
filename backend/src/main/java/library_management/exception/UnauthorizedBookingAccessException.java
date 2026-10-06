package library_management.exception;

public class UnauthorizedBookingAccessException extends RuntimeException {
    public UnauthorizedBookingAccessException() {
        super("You are not authorized to access this booking.");
    }
}