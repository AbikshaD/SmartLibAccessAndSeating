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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MaintenanceTicket> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        MaintenanceTicket ticket = ticketService.createTicket(
                new MaintenanceTicketService.CreateTicketRequest(request.team(), request.description(),
                        request.floor(), request.priority()));
        return ResponseEntity.created(URI.create("/maintenance/tickets/" + ticket.getId())).body(ticket);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('MAINTENANCE')")
    public ResponseEntity<MaintenanceTicketService.Dashboard> getDashboard(Authentication authentication) {
        return ResponseEntity.ok(ticketService.getDashboard(authentication.getName()));
    }

    @GetMapping("/tickets/{ticketId}")
    @PreAuthorize("hasRole('MAINTENANCE')")
    public ResponseEntity<MaintenanceTicket> getTicket(@PathVariable String ticketId,
            Authentication authentication) {
        return ResponseEntity.ok(ticketService.getTicket(ticketId, authentication.getName()));
    }

    @PutMapping("/tickets/{ticketId}/status")
    @PreAuthorize("hasRole('MAINTENANCE')")
    public ResponseEntity<MaintenanceTicket> updateStatus(@PathVariable String ticketId,
            @Valid @RequestBody UpdateStatusRequest request, Authentication authentication) {
        return ResponseEntity.ok(ticketService.updateStatus(ticketId, authentication.getName(), request.status()));
    }

    public record CreateTicketRequest(
            @NotNull MaintenanceTeam team,
            @NotBlank @Size(max = 2000) String description,
            @NotBlank @Size(max = 100) String floor,
            @NotNull MaintenanceTicket.Priority priority) { }

    public record UpdateStatusRequest(@NotNull MaintenanceTicket.Status status) { }
}
