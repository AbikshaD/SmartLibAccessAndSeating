package library_management.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BookingExpiryScheduler {

    private final BookingService bookingService;

    public BookingExpiryScheduler(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelay = 10_000L)
    public void completeExpiredBookings() {
        bookingService.completeExpiredBookings();
    }
}
