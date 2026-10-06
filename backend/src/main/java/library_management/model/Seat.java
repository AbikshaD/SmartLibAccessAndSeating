package library_management.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "seats")
@CompoundIndex(name = "floor_seat_number_unique", def = "{'floor': 1, 'seatNumber': 1}", unique = true)
public class Seat {

    public enum Status {
        AVAILABLE,
        OCCUPIED,
        BOOKED
    }

    @Id
    private String id;

    @NotBlank
    private String seatNumber;

    @NotBlank
    private String floor;

    @NotNull
    private Status status = Status.AVAILABLE;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}