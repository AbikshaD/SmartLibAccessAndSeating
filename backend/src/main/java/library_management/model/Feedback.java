package library_management.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Document(collection = "feedback")
public class Feedback {

    public enum Category {
        SEATING,
        BOOKING,
        BOOKS,
        FACILITIES,
        CLEANLINESS,
        STAFF,
        OTHER
    }

    public enum Status {
        PENDING,
        REVIEWED,
        RESOLVED
    }

    @Id
    private String id;

    private String studentId;

    @NotNull
    private Category category;

    @Min(1)
    @Max(5)
    private int rating;

    @NotBlank
    @Size(max = 2000)
    private String comment;

    private Status status;
    private String adminResponse;
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getAdminResponse() { return adminResponse; }
    public void setAdminResponse(String adminResponse) { this.adminResponse = adminResponse; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}