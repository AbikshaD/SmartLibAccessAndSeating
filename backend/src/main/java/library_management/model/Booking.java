package library_management.model;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "bookings")
public class Booking {

    public enum Status {
        CONFIRMED,
        CANCELLED,
        COMPLETED
    }

    @Id
    private String id;
    private String studentId;
    private String seatId;
    @JsonIgnore
    private String seatDocumentId;
    private String seatNumber;
    private String floor;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Status status;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getSeatId() { return seatId; }
    public void setSeatId(String seatId) { this.seatId = seatId; }
    public String getSeatDocumentId() { return seatDocumentId; }
    public void setSeatDocumentId(String seatDocumentId) { this.seatDocumentId = seatDocumentId; }
    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }
    public LocalDate getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}