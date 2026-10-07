package library_management.exception;

public class FeedbackNotFoundException extends RuntimeException {
    public FeedbackNotFoundException(String feedbackId) {
        super("Feedback not found: " + feedbackId);
    }
}