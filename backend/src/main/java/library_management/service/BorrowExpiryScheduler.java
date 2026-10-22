package library_management.service;

import java.time.LocalDate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BorrowExpiryScheduler {

    private final BorrowService borrowService;

    public BorrowExpiryScheduler(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    // Run every day at 02:00 AM to mark overdue borrows
    @Scheduled(cron = "0 0 2 * * *")
    public void processOverdueBorrows() {
        borrowService.markOverdue();
    }

    // Run every day at 03:00 AM to flag due-soon notifications (2 days before due date)
    @Scheduled(cron = "0 0 3 * * *")
    public void processDueSoonNotifications() {
        borrowService.notifyDueSoon();
    }
}
