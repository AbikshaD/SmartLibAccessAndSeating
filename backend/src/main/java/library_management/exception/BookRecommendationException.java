package library_management.exception;

import org.springframework.http.HttpStatus;

public class BookRecommendationException extends RuntimeException {

    private final HttpStatus status;

    public BookRecommendationException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
