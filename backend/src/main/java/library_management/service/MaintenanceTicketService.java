package library_management.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import library_management.dto.AIClassificationResult;
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
    private final GroqAIService groqAIService;

    public MaintenanceTicketService(MaintenanceTicketRepository ticketRepository, UserRepository userRepository,
            GroqAIService groqAIService) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.groqAIService = groqAIService;
    }

    public MaintenanceTicket createTicket(CreateTicketRequest request) {
        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.setTeam(request.team());
        ticket.setDescription(request.description().trim());
        ticket.setFloor(request.floor().trim());
        ticket.setPriority(request.priority());
        ticket.setStatus(MaintenanceTicket.Status.OPEN);
        ticket.setSubmittedAt(LocalDateTime.now());
        assignEligibleStaff(ticket, request.team());
        return ticketRepository.save(ticket);
    }

    public MaintenanceTicket reportIssue(String studentId, ReportIssueRequest request) {
        return reportIssue(studentId, request, false);
    }

    public MaintenanceTicket reportIssue(String studentId, ReportIssueRequest request, boolean isStudent) {
        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.setReportedBy(studentId);
        ticket.setDescription(request.description().trim());
        ticket.setFloor(request.floor() != null ? request.floor().trim() : "General Library");
        ticket.setSubmittedAt(LocalDateTime.now());

        if (isStudent) {
            Optional<AIClassificationResult> classificationOpt = groqAIService.classify(ticket.getDescription());
            if (classificationOpt.isPresent()) {
                AIClassificationResult result = classificationOpt.get();
                ticket.setTeam(result.category());
                ticket.setPriority(result.priority());
                ticket.setAiCategory(result.category().name());
                ticket.setClassificationReason(result.reason());
                ticket.setStatus(MaintenanceTicket.Status.OPEN);
                assignEligibleStaff(ticket, result.category());
            } else {
                ticket.setTeam(null);
                ticket.setPriority(request.priority() != null ? request.priority() : MaintenanceTicket.Priority.MEDIUM);
                ticket.setAiCategory(null);
                ticket.setClassificationReason("Pending manual classification: AI service was unavailable or returned invalid output.");
                ticket.setStatus(MaintenanceTicket.Status.PENDING_CLASSIFICATION);
            }
        } else {
            ticket.setPriority(request.priority() != null ? request.priority() : MaintenanceTicket.Priority.MEDIUM);
            ticket.setStatus(MaintenanceTicket.Status.OPEN);
        }

        return ticketRepository.save(ticket);
    }

    public MaintenanceTicket createTicketFromFeedback(String studentId, String comment, String floor, String feedbackId) {
        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.setReportedBy(studentId);
        ticket.setFeedbackId(feedbackId);
        ticket.setDescription(comment.trim());
        ticket.setFloor(floor != null && !floor.isBlank() ? floor.trim() : "General Library");
        ticket.setSubmittedAt(LocalDateTime.now());

        Optional<AIClassificationResult> classificationOpt = groqAIService.classify(ticket.getDescription());
        if (classificationOpt.isPresent()) {
            AIClassificationResult result = classificationOpt.get();
            ticket.setTeam(result.category());
            ticket.setPriority(result.priority());
            ticket.setAiCategory(result.category().name());
            ticket.setClassificationReason(result.reason());
            ticket.setStatus(MaintenanceTicket.Status.OPEN);
            assignEligibleStaff(ticket, result.category());
        } else {
            ticket.setTeam(null);
            ticket.setPriority(MaintenanceTicket.Priority.MEDIUM);
            ticket.setAiCategory(null);
            ticket.setClassificationReason("Pending manual classification: AI service was unavailable or returned invalid output.");
            ticket.setStatus(MaintenanceTicket.Status.PENDING_CLASSIFICATION);
        }

        return ticketRepository.save(ticket);
    }

    public List<MaintenanceTicket> getMyReports(String studentId) {
        return ticketRepository.findByReportedByOrderBySubmittedAtDesc(studentId);
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
        if (ticket.getStatus() == MaintenanceTicket.Status.PENDING_CLASSIFICATION) {
            ticket.setStatus(MaintenanceTicket.Status.OPEN);
        }
        assignEligibleStaff(ticket, team);
        return ticketRepository.save(ticket);
    }

    public MaintenanceTicket classifyTicket(String ticketId) {
        MaintenanceTicket ticket = findTicket(ticketId);
        Optional<AIClassificationResult> classificationOpt = groqAIService.classify(ticket.getDescription());
        if (classificationOpt.isPresent()) {
            AIClassificationResult result = classificationOpt.get();
            ticket.setTeam(result.category());
            ticket.setPriority(result.priority());
            ticket.setAiCategory(result.category().name());
            ticket.setClassificationReason(result.reason());
            ticket.setStatus(MaintenanceTicket.Status.OPEN);
            assignEligibleStaff(ticket, result.category());
            return ticketRepository.save(ticket);
        }
        return ticket;
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
        MaintenanceTicket ticket = findTicket(ticketId);
        if (ticket.getReportedBy() != null && ticket.getReportedBy().equals(studentId)) {
            return ticket;
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

    private void assignEligibleStaff(MaintenanceTicket ticket, MaintenanceTeam team) {
        if (team == null) {
            ticket.setAssignedStaffId(null);
            return;
        }
        List<User> staffMembers = userRepository.findByRoleOrderByNameAsc("MAINTENANCE").stream()
                .filter(user -> user.getMaintenanceTeam() == team && user.isActive() && !user.isBlocked())
                .toList();

        if (staffMembers.isEmpty()) {
            ticket.setAssignedStaffId(null);
            return;
        }

        List<MaintenanceTicket.Status> activeStatuses = List.of(
                MaintenanceTicket.Status.OPEN,
                MaintenanceTicket.Status.IN_PROGRESS
        );

        User selectedStaff = null;
        long minWorkload = Long.MAX_VALUE;

        for (User staff : staffMembers) {
            long workload = ticketRepository.countByAssignedStaffIdAndStatusIn(staff.getStudentId(), activeStatuses);
            if (workload < minWorkload) {
                minWorkload = workload;
                selectedStaff = staff;
            }
        }

        if (selectedStaff != null) {
            ticket.setAssignedStaffId(selectedStaff.getStudentId());
        }
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
