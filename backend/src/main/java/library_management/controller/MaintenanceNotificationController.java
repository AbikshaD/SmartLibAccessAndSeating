package library_management.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import library_management.service.MaintenanceNotificationService;
import library_management.service.MaintenanceNotificationService.NotificationResponse;

@RestController
@RequestMapping("/maintenance/notifications")
@PreAuthorize("hasRole('MAINTENANCE')")
public class MaintenanceNotificationController {

    private final MaintenanceNotificationService notificationService;

    public MaintenanceNotificationController(MaintenanceNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(Authentication authentication) {
        return ResponseEntity.ok(notificationService.getNotifications(authentication.getName()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(Authentication authentication) {
        return ResponseEntity.ok(new UnreadCountResponse(
                notificationService.getUnreadCount(authentication.getName())));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markRead(@PathVariable String notificationId,
            Authentication authentication) {
        return ResponseEntity.ok(notificationService.markRead(notificationId, authentication.getName()));
    }

    public record UnreadCountResponse(long unreadCount) { }
}
