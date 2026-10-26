package library_management.exception;

public class MaintenanceTeamRequiredException extends RuntimeException {
    public MaintenanceTeamRequiredException() {
        super("A maintenance team must be assigned to access the maintenance dashboard.");
    }
}
