package library_management.exception;

public class MaintenanceTicketNotFoundException extends RuntimeException {
    public MaintenanceTicketNotFoundException(String ticketId) {
        super("Maintenance ticket not found: " + ticketId);
    }
}
