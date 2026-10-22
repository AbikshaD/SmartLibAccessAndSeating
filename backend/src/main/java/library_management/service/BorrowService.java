package library_management.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import library_management.model.Borrow;
import library_management.model.Book;
import library_management.repository.BorrowRepository;
import library_management.repository.BookRepository;
import library_management.repository.UserRepository;
import library_management.model.User;

@Service
public class BorrowService {

    private final BorrowRepository borrowRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public BorrowService(BorrowRepository borrowRepository, BookRepository bookRepository, UserRepository userRepository) {
        this.borrowRepository = borrowRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Borrow requestBorrow(String studentId, String bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));
        User user = userRepository.findById(studentId).orElse(null);
        Borrow borrow = new Borrow();
        borrow.setStudentId(studentId);
        borrow.setStudentName(user != null ? user.getName() : studentId);
        borrow.setBookId(bookId);
        borrow.setBookTitle(book.getTitle());
        borrow.setBookAuthor(book.getAuthor());
        borrow.setBookGenre(book.getGenre());
        borrow.setRequestedAt(LocalDate.now());
        borrow.setStatus(Borrow.Status.PENDING);
        borrow.setDueSoonNotified(false);
        return borrowRepository.save(borrow);
    }

    public List<Borrow> getMyBorrows(String studentId) {
        return borrowRepository.findByStudentId(studentId);
    }

    public List<Borrow> getAllBorrows() {
        return borrowRepository.findAll();
    }

    @Transactional
    public Borrow approveBorrow(String borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new IllegalArgumentException("Borrow request not found"));
        LocalDate approvedAt = LocalDate.now();
        borrow.setStatus(Borrow.Status.APPROVED);
        borrow.setApprovedAt(approvedAt);
        borrow.setDueDate(approvedAt.plusDays(7));
        return borrowRepository.save(borrow);
    }

    @Transactional
    public Borrow rejectBorrow(String borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new IllegalArgumentException("Borrow request not found"));
        borrow.setStatus(Borrow.Status.REJECTED);
        return borrowRepository.save(borrow);
    }

    @Transactional
    public Borrow markReturned(String borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new IllegalArgumentException("Borrow request not found"));
        borrow.setReturnedAt(LocalDate.now());
        borrow.setStatus(Borrow.Status.RETURNED);
        // fine calculation
        if (borrow.getDueDate() != null && LocalDate.now().isAfter(borrow.getDueDate())) {
            long daysLate = ChronoUnit.DAYS.between(borrow.getDueDate(), LocalDate.now());
            double fine = daysLate * 10.0; // ₹10 per day
            borrow.setFineAmount(fine);
        } else {
            borrow.setFineAmount(0.0);
        }
        return borrowRepository.save(borrow);
    }

    @Transactional
    public void markOverdue() {
        LocalDate today = LocalDate.now();
        List<Borrow> overdue = borrowRepository.findByStatusAndDueDateBefore(Borrow.Status.APPROVED, today);
        for (Borrow b : overdue) {
            b.setStatus(Borrow.Status.OVERDUE);
        }
        borrowRepository.saveAll(overdue);
    }

    @Transactional
    public void notifyDueSoon() {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(2);
        List<Borrow> upcoming = borrowRepository.findByStatus(Borrow.Status.APPROVED);
        for (Borrow b : upcoming) {
            if (!Boolean.TRUE.equals(b.getDueSoonNotified()) && b.getDueDate() != null && !b.getDueDate().isAfter(threshold)) {
                // In a real system, send notification (e.g., email/SMS). Here we just flag it.
                b.setDueSoonNotified(true);
            }
        }
        borrowRepository.saveAll(upcoming);
    }
}
