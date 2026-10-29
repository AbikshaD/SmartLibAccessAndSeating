package library_management.exception;

public class MaintenanceNotificationNotFoundException extends RuntimeException {
    public MaintenanceNotificationNotFoundException(String notificationId) {
        super("Maintenance notification not found: " + notificationId);
    }
}
