package library_management.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Document(collection = "maintenance_tickets")
public class MaintenanceTicket {

    public enum Status {
        OPEN,
        IN_PROGRESS,
        RESOLVED,
        PENDING_CLASSIFICATION
    }

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }

    @Id
    private String id;

    private MaintenanceTeam team;

    private String reportedBy;

    private String feedbackId;

    @NotBlank
    @Size(max = 2000)
    private String description;

    @Size(max = 100)
    private String floor;

    private Priority priority;

    private String aiCategory;
    private String classificationReason;
    private String assignedStaffId;

    private Status status;
    private LocalDateTime submittedAt;
    private String resolutionNotes;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public MaintenanceTeam getTeam() { return team; }
    public void setTeam(MaintenanceTeam team) { this.team = team; }
    public String getReportedBy() { return reportedBy; }
    public void setReportedBy(String reportedBy) { this.reportedBy = reportedBy; }
    public String getFeedbackId() { return feedbackId; }
    public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public String getAiCategory() { return aiCategory; }
    public void setAiCategory(String aiCategory) { this.aiCategory = aiCategory; }
    public String getClassificationReason() { return classificationReason; }
    public void setClassificationReason(String classificationReason) { this.classificationReason = classificationReason; }
    public String getAssignedStaffId() { return assignedStaffId; }
    public void setAssignedStaffId(String assignedStaffId) { this.assignedStaffId = assignedStaffId; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
}
