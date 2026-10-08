package library_management.exception;

public class AnnouncementNotFoundException extends RuntimeException {

    public AnnouncementNotFoundException(String id) {
        super("Announcement not found: " + id);
    }
}
