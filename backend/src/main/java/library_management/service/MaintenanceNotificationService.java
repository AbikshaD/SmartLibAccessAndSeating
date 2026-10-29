package library_management.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import library_management.exception.MaintenanceNotificationNotFoundException;
import library_management.model.MaintenanceNotification;
import library_management.model.MaintenanceTeam;
import library_management.model.MaintenanceTicket;
import library_management.model.User;
import library_management.repository.MaintenanceNotificationRepository;
import library_management.repository.UserRepository;

@Service
public class MaintenanceNotificationService {

    private final MaintenanceNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MaintenanceNotificationService(MaintenanceNotificationRepository notificationRepository,
            UserRepository userRepository, SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyAssignedTeam(MaintenanceTicket ticket) {
        MaintenanceTeam team = ticket.getTeam();
        if (team == null || ticket.getAssignedStaffId() == null) {
            return;
        }

        List<User> recipients = userRepository.findByRoleAndMaintenanceTeamAndActiveTrueAndBlockedFalse(
                "MAINTENANCE", team);
        for (User recipient : recipients) {
            MaintenanceNotification notification = new MaintenanceNotification();
            notification.setRecipientId(recipient.getStudentId());
            notification.setTicketId(ticket.getId());
            notification.setTeam(team);
            notification.setDescription(ticket.getDescription());
            notification.setPriority(ticket.getPriority());
            notification.setCreatedAt(LocalDateTime.now());
            notification.setRead(false);

            MaintenanceNotification savedNotification = notificationRepository.save(notification);
            messagingTemplate.convertAndSendToUser(recipient.getStudentId(), "/queue/notifications",
                    NotificationResponse.from(savedNotification));
        }
    }

    public List<NotificationResponse> getNotifications(String recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public long getUnreadCount(String recipientId) {
        return notificationRepository.countByRecipientIdAndReadFalse(recipientId);
    }

    public NotificationResponse markRead(String notificationId, String recipientId) {
        MaintenanceNotification notification = notificationRepository
                .findByIdAndRecipientId(notificationId, recipientId)
                .orElseThrow(() -> new MaintenanceNotificationNotFoundException(notificationId));
        notification.setRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    public record NotificationResponse(String id, String ticketId, MaintenanceTeam team, String description,
            MaintenanceTicket.Priority priority, LocalDateTime createdAt, boolean read) {
        private static NotificationResponse from(MaintenanceNotification notification) {
            return new NotificationResponse(notification.getId(), notification.getTicketId(),
                    notification.getTeam(), notification.getDescription(), notification.getPriority(),
                    notification.getCreatedAt(), notification.isRead());
        }
    }
}
