package library_management.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import library_management.exception.MaintenanceTeamRequiredException;
import library_management.exception.MaintenanceTicketNotFoundException;
import library_management.exception.UserNotFoundException;
import library_management.model.MaintenanceTeam;
import library_management.model.MaintenanceTicket;
import library_management.model.User;
import library_management.repository.MaintenanceTicketRepository;
import library_management.repository.UserRepository;

@Service
public class MaintenanceTicketService {

    private final MaintenanceTicketRepository ticketRepository;
    private final UserRepository userRepository;

    public MaintenanceTicketService(MaintenanceTicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    public MaintenanceTicket createTicket(CreateTicketRequest request) {
        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.setTeam(request.team());
        ticket.setDescription(request.description().trim());
        ticket.setFloor(request.floor().trim());
        ticket.setPriority(request.priority());
        ticket.setStatus(MaintenanceTicket.Status.OPEN);
        ticket.setSubmittedAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    public MaintenanceTicket reportIssue(String studentId, ReportIssueRequest request) {
        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.setReportedBy(studentId);
        ticket.setDescription(request.description().trim());
        ticket.setFloor(request.floor().trim());
        ticket.setPriority(request.priority());
        ticket.setStatus(MaintenanceTicket.Status.OPEN);
        ticket.setSubmittedAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    public List<MaintenanceTicket> getAllTickets() {
        return ticketRepository.findAllByOrderBySubmittedAtDesc();
    }

    public MaintenanceTicket getTicketForManagement(String ticketId) {
        return findTicket(ticketId);
    }

    public MaintenanceTicket assignTeam(String ticketId, MaintenanceTeam team) {
        MaintenanceTicket ticket = findTicket(ticketId);
        ticket.setTeam(team);
        return ticketRepository.save(ticket);
    }

    public Dashboard getDashboard(String studentId) {
        MaintenanceTeam team = getMaintenanceTeam(studentId);
        List<MaintenanceTicket> tickets = ticketRepository.findByTeamOrderBySubmittedAtDesc(team);
        long openCount = tickets.stream().filter(ticket -> ticket.getStatus() == MaintenanceTicket.Status.OPEN).count();
        long inProgressCount = tickets.stream()
                .filter(ticket -> ticket.getStatus() == MaintenanceTicket.Status.IN_PROGRESS).count();
        long resolvedCount = tickets.stream()
                .filter(ticket -> ticket.getStatus() == MaintenanceTicket.Status.RESOLVED).count();
        return new Dashboard(team, tickets, openCount, inProgressCount, resolvedCount);
    }

    public MaintenanceTicket getTicket(String ticketId, String studentId, boolean maintenanceAdmin) {
        if (maintenanceAdmin) {
            return findTicket(ticketId);
        }
        MaintenanceTeam team = getMaintenanceTeam(studentId);
        return findTicketForTeam(ticketId, team);
    }

    public MaintenanceTicket updateStatus(String ticketId, String studentId, boolean maintenanceAdmin,
            MaintenanceTicket.Status status, String resolutionNotes) {
        MaintenanceTicket ticket;
        if (maintenanceAdmin) {
            ticket = findTicket(ticketId);
        } else {
            MaintenanceTeam team = getMaintenanceTeam(studentId);
            ticket = findTicketForTeam(ticketId, team);
        }
        ticket.setStatus(status);
        if (resolutionNotes != null) {
            ticket.setResolutionNotes(resolutionNotes.trim());
        }
        return ticketRepository.save(ticket);
    }

    private MaintenanceTeam getMaintenanceTeam(String studentId) {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new UserNotFoundException(studentId));
        if (user.getMaintenanceTeam() == null) {
            throw new MaintenanceTeamRequiredException();
        }
        return user.getMaintenanceTeam();
    }

    private MaintenanceTicket findTicketForTeam(String ticketId, MaintenanceTeam team) {
        MaintenanceTicket ticket = findTicket(ticketId);
        if (ticket.getTeam() != team) {
            throw new MaintenanceTicketNotFoundException(ticketId);
        }
        return ticket;
    }

    private MaintenanceTicket findTicket(String ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new MaintenanceTicketNotFoundException(ticketId));
    }

    public record CreateTicketRequest(MaintenanceTeam team, String description, String floor,
            MaintenanceTicket.Priority priority) { }

    public record ReportIssueRequest(String description, String floor, MaintenanceTicket.Priority priority) { }

    public record Dashboard(MaintenanceTeam team, List<MaintenanceTicket> tickets,
            long openCount, long inProgressCount, long resolvedCount) { }
}
