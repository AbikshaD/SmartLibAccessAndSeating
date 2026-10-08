package library_management.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import library_management.model.Announcement;
import library_management.service.AnnouncementService;

@RestController
@RequestMapping("/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<List<Announcement>> getAllAnnouncements(Authentication authentication) {
        // Admins can see all announcements, students see active announcements
        if (isAdmin(authentication)) {
            return ResponseEntity.ok(announcementService.getAllAnnouncements());
        }
        return ResponseEntity.ok(announcementService.getActiveAnnouncements());
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<List<Announcement>> getActiveAnnouncements() {
        return ResponseEntity.ok(announcementService.getActiveAnnouncements());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<Announcement> getAnnouncementById(@PathVariable String id) {
        return ResponseEntity.ok(announcementService.getAnnouncementById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Announcement> createAnnouncement(
            @Valid @RequestBody CreateAnnouncementRequest request,
            Authentication authentication) {
        String adminId = authentication != null ? authentication.getName() : "ADMIN";
        Announcement created = announcementService.createAnnouncement(
                adminId,
                request.title(),
                request.content(),
                request.type(),
                request.priority()
        );
        return ResponseEntity.created(URI.create("/announcements/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Announcement> updateAnnouncement(
            @PathVariable String id,
            @RequestBody UpdateAnnouncementRequest request) {
        Announcement updated = announcementService.updateAnnouncement(
                id,
                request.title(),
                request.content(),
                request.type(),
                request.priority(),
                request.active()
        );
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable String id) {
        announcementService.deleteAnnouncement(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    public record CreateAnnouncementRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 3000) String content,
            String type,
            String priority
    ) {}

    public record UpdateAnnouncementRequest(
            String title,
            String content,
            String type,
            String priority,
            Boolean active
    ) {}
}
