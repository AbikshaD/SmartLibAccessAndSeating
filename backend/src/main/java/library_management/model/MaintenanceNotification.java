package library_management.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "maintenance_notifications")
public class MaintenanceNotification {

    @Id
    private String id;

    private String recipientId;
    private String ticketId;
    private MaintenanceTeam team;
    private String description;
    private MaintenanceTicket.Priority priority;
    private LocalDateTime createdAt;
    private boolean read;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }
    public MaintenanceTeam getTeam() { return team; }
    public void setTeam(MaintenanceTeam team) { this.team = team; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public MaintenanceTicket.Priority getPriority() { return priority; }
    public void setPriority(MaintenanceTicket.Priority priority) { this.priority = priority; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
