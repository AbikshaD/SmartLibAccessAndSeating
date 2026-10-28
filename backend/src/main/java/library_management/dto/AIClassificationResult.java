package library_management.dto;

import library_management.model.MaintenanceTeam;
import library_management.model.MaintenanceTicket;

public record AIClassificationResult(
        MaintenanceTeam category,
        MaintenanceTicket.Priority priority,
        String reason
) { }
