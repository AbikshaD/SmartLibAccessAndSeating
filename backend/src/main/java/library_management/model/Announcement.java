package library_management.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Document(collection = "announcements")
public class Announcement {

    @Id
    private String id;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 3000)
    private String content;

    private String type = "GENERAL"; // CLOSURE, URGENT, MAINTENANCE, GENERAL, EVENT

    private String priority = "NORMAL"; // HIGH, MEDIUM, NORMAL

    private String postedBy;

    private boolean active = true;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Announcement() {
    }

    public Announcement(String title, String content, String type, String priority, String postedBy) {
        this.title = title;
        this.content = content;
        this.type = type != null ? type : "GENERAL";
        this.priority = priority != null ? priority : "NORMAL";
        this.postedBy = postedBy;
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(String postedBy) {
        this.postedBy = postedBy;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
