package library_management.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "borrows")
public class Borrow {
    public enum Status {
        PENDING,
        APPROVED,
        REJECTED,
        RETURNED,
        OVERDUE
    }

    @Id
    private String id;
    private String studentId;
    private String studentName;
    private String bookId;
    private String bookTitle;
    private String bookAuthor;
    private String bookGenre;
    private LocalDate requestedAt;
    private LocalDate approvedAt;
    private LocalDate dueDate;
    private LocalDate returnedAt;
    private Status status;
    private Double fineAmount;
    private Boolean dueSoonNotified;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }
    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
    public String getBookAuthor() { return bookAuthor; }
    public void setBookAuthor(String bookAuthor) { this.bookAuthor = bookAuthor; }
    public String getBookGenre() { return bookGenre; }
    public void setBookGenre(String bookGenre) { this.bookGenre = bookGenre; }
    public LocalDate getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDate requestedAt) { this.requestedAt = requestedAt; }
    public LocalDate getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDate approvedAt) { this.approvedAt = approvedAt; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDate returnedAt) { this.returnedAt = returnedAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Double getFineAmount() { return fineAmount; }
    public void setFineAmount(Double fineAmount) { this.fineAmount = fineAmount; }
    public Boolean getDueSoonNotified() { return dueSoonNotified; }
    public void setDueSoonNotified(Boolean dueSoonNotified) { this.dueSoonNotified = dueSoonNotified; }
}
