package library_management.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import library_management.model.MaintenanceTeam;
import library_management.model.MaintenanceTicket;
import library_management.service.MaintenanceTicketService;

@RestController
@RequestMapping("/maintenance")
public class MaintenanceController {

    private final MaintenanceTicketService ticketService;

    public MaintenanceController(MaintenanceTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/tickets")
    @PreAuthorize("hasRole('MAINTENANCE_ADMIN')")
    public ResponseEntity<MaintenanceTicket> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        MaintenanceTicket ticket = ticketService.createTicket(
                new MaintenanceTicketService.CreateTicketRequest(request.team(), request.description(),
                        request.floor(), request.priority()));
        return ResponseEntity.created(URI.create("/maintenance/tickets/" + ticket.getId())).body(ticket);
    }

    @PostMapping("/reports")
    @PreAuthorize("hasRole('MAINTENANCE')")
    public ResponseEntity<MaintenanceTicket> reportIssue(@Valid @RequestBody ReportIssueRequest request,
            Authentication authentication) {
        MaintenanceTicket ticket = ticketService.reportIssue(authentication.getName(),
                new MaintenanceTicketService.ReportIssueRequest(request.description(), request.floor(),
                        request.priority()));
        return ResponseEntity.created(URI.create("/maintenance/tickets/" + ticket.getId())).body(ticket);
    }

    @GetMapping("/management/tickets")
    @PreAuthorize("hasRole('MAINTENANCE_ADMIN')")
    public ResponseEntity<java.util.List<MaintenanceTicket>> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    @GetMapping("/management/tickets/{ticketId}")
    @PreAuthorize("hasRole('MAINTENANCE_ADMIN')")
    public ResponseEntity<MaintenanceTicket> getManagedTicket(@PathVariable String ticketId) {
        return ResponseEntity.ok(ticketService.getTicketForManagement(ticketId));
    }

    @PutMapping("/tickets/{ticketId}/team")
    @PreAuthorize("hasRole('MAINTENANCE_ADMIN')")
    public ResponseEntity<MaintenanceTicket> assignTeam(@PathVariable String ticketId,
            @Valid @RequestBody AssignTeamRequest request) {
        return ResponseEntity.ok(ticketService.assignTeam(ticketId, request.team()));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('MAINTENANCE')")
    public ResponseEntity<MaintenanceTicketService.Dashboard> getDashboard(Authentication authentication) {
        return ResponseEntity.ok(ticketService.getDashboard(authentication.getName()));
    }

    @GetMapping("/tickets/{ticketId}")
    @PreAuthorize("hasAnyRole('MAINTENANCE', 'MAINTENANCE_ADMIN')")
    public ResponseEntity<MaintenanceTicket> getTicket(@PathVariable String ticketId,
            Authentication authentication) {
        return ResponseEntity.ok(ticketService.getTicket(ticketId, authentication.getName(),
                hasMaintenanceAdminRole(authentication)));
    }

    @PutMapping("/tickets/{ticketId}/status")
    @PreAuthorize("hasAnyRole('MAINTENANCE', 'MAINTENANCE_ADMIN')")
    public ResponseEntity<MaintenanceTicket> updateStatus(@PathVariable String ticketId,
            @Valid @RequestBody UpdateStatusRequest request, Authentication authentication) {
        return ResponseEntity.ok(ticketService.updateStatus(ticketId, authentication.getName(),
                hasMaintenanceAdminRole(authentication), request.status(), request.resolutionNotes()));
    }

    private boolean hasMaintenanceAdminRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_MAINTENANCE_ADMIN"));
    }

    public record CreateTicketRequest(
            @NotNull MaintenanceTeam team,
            @NotBlank @Size(max = 2000) String description,
            @NotBlank @Size(max = 100) String floor,
            @NotNull MaintenanceTicket.Priority priority) { }

    public record UpdateStatusRequest(
            @NotNull MaintenanceTicket.Status status,
            @Size(max = 2000) String resolutionNotes) { }

    public record AssignTeamRequest(@NotNull MaintenanceTeam team) { }

    public record ReportIssueRequest(
            @NotBlank @Size(max = 2000) String description,
            @NotBlank @Size(max = 100) String floor,
            @NotNull MaintenanceTicket.Priority priority) { }
}
