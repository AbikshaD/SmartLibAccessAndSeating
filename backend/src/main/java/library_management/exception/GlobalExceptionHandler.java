package library_management.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(SeatNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleSeatNotFound(SeatNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(SeatOccupiedException.class)
    public ResponseEntity<Map<String, String>> handleSeatOccupied(SeatOccupiedException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler({SeatAlreadyBookedException.class, DuplicateSeatGenerationException.class,
            BookingCannotBeCancelledException.class, FloorCapacityConflictException.class})
    public ResponseEntity<Map<String, String>> handleBookingConflict(RuntimeException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleBookingNotFound(BookingNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({InvalidFloorException.class, InvalidBookingRequestException.class})
    public ResponseEntity<Map<String, String>> handleInvalidRequest(RuntimeException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(UnauthorizedBookingAccessException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorizedBookingAccess(
            UnauthorizedBookingAccessException exception) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateEmail(DuplicateEmailException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleInvalidInput(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse("Invalid input");
        return error(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }
}