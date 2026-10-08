package library_management.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import library_management.exception.AnnouncementNotFoundException;
import library_management.model.Announcement;
import library_management.repository.AnnouncementRepository;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    public List<Announcement> getAllAnnouncements() {
        return announcementRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Announcement> getActiveAnnouncements() {
        return announcementRepository.findByActiveTrueOrderByCreatedAtDesc();
    }

    public Announcement getAnnouncementById(String id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new AnnouncementNotFoundException(id));
    }

    public Announcement createAnnouncement(String postedBy, String title, String content, String type, String priority) {
        Announcement announcement = new Announcement();
        announcement.setTitle(title.trim());
        announcement.setContent(content.trim());
        announcement.setType(type != null && !type.isBlank() ? type.trim().toUpperCase() : "GENERAL");
        announcement.setPriority(priority != null && !priority.isBlank() ? priority.trim().toUpperCase() : "NORMAL");
        announcement.setPostedBy(postedBy);
        announcement.setActive(true);
        announcement.setCreatedAt(LocalDateTime.now());
        announcement.setUpdatedAt(LocalDateTime.now());
        return announcementRepository.save(announcement);
    }

    public Announcement updateAnnouncement(String id, String title, String content, String type, String priority, Boolean active) {
        Announcement announcement = getAnnouncementById(id);
        if (title != null && !title.isBlank()) {
            announcement.setTitle(title.trim());
        }
        if (content != null && !content.isBlank()) {
            announcement.setContent(content.trim());
        }
        if (type != null && !type.isBlank()) {
            announcement.setType(type.trim().toUpperCase());
        }
        if (priority != null && !priority.isBlank()) {
            announcement.setPriority(priority.trim().toUpperCase());
        }
        if (active != null) {
            announcement.setActive(active);
        }
        announcement.setUpdatedAt(LocalDateTime.now());
        return announcementRepository.save(announcement);
    }

    public void deleteAnnouncement(String id) {
        Announcement announcement = getAnnouncementById(id);
        announcementRepository.delete(announcement);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedInitialAnnouncementsIfEmpty() {
        try {
            if (announcementRepository.count() == 0) {
                Announcement closureNotice = new Announcement(
                        "Early Library Closure Today at 6:00 PM",
                        "Please note that the central library and silent reading floors will close early at 6:00 PM today due to scheduled electrical facility upgrades. All check-out desks and study rooms will close at 5:45 PM. Regular operating hours resume tomorrow at 8:00 AM.",
                        "CLOSURE",
                        "HIGH",
                        "admin"
                );

                Announcement examNotice = new Announcement(
                        "Extended 24/7 Hours for Examination Week",
                        "To support students during upcoming final exams, Floor 2 and Floor 3 study halls will remain open 24/7 starting next Monday. Silent zones and high-speed Wi-Fi will be fully accessible.",
                        "GENERAL",
                        "NORMAL",
                        "admin"
                );

                Announcement maintenanceNotice = new Announcement(
                        "Wi-Fi Network Maintenance Notice",
                        "Brief Wi-Fi connectivity interruptions may occur on Floor 1 between 2:00 PM and 3:00 PM this Friday while campus IT upgrades network routers.",
                        "MAINTENANCE",
                        "MEDIUM",
                        "admin"
                );

                announcementRepository.saveAll(List.of(closureNotice, examNotice, maintenanceNotice));
            }
        } catch (Exception e) {
            System.err.println("Note: Could not automatically seed announcements: " + e.getMessage());
        }
    }
}
