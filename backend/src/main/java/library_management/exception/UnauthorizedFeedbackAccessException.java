package library_management.exception;

public class UnauthorizedFeedbackAccessException extends RuntimeException {
    public UnauthorizedFeedbackAccessException() {
        super("You are not authorized to access this feedback.");
    }
}