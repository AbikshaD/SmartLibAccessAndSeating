package library_management.controller;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import library_management.model.Borrow;
import library_management.service.BorrowService;

@RestController
@RequestMapping("/borrows")
public class BorrowController {

    private final BorrowService borrowService;

    public BorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Borrow> requestBorrow(@Valid @RequestBody RequestBorrow request, Authentication authentication) {
        Borrow borrow = borrowService.requestBorrow(authentication.getName(), request.bookId());
        return ResponseEntity.ok(borrow);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<List<Borrow>> getMyBorrows(Authentication authentication) {
        return ResponseEntity.ok(borrowService.getMyBorrows(authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Borrow>> getAllBorrows() {
        return ResponseEntity.ok(borrowService.getAllBorrows());
    }

    @PutMapping("/{borrowId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Borrow> approveBorrow(@PathVariable String borrowId) {
        return ResponseEntity.ok(borrowService.approveBorrow(borrowId));
    }

    @PutMapping("/{borrowId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Borrow> rejectBorrow(@PathVariable String borrowId) {
        return ResponseEntity.ok(borrowService.rejectBorrow(borrowId));
    }

    @PutMapping("/{borrowId}/return")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<Borrow> returnBorrow(@PathVariable String borrowId, Authentication authentication) {
        // In a real system we could verify the user matches the borrower, omitted for brevity
        return ResponseEntity.ok(borrowService.markReturned(borrowId));
    }

    public record RequestBorrow(@Valid String bookId) {}
}
