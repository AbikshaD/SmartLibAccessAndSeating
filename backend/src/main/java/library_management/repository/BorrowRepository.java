package library_management.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import library_management.model.Borrow;
import java.util.List;

public interface BorrowRepository extends MongoRepository<Borrow, String> {
    List<Borrow> findByStudentId(String studentId);
    List<Borrow> findByStatus(Borrow.Status status);
    List<Borrow> findByStatusAndDueDateBefore(Borrow.Status status, java.time.LocalDate date);
    List<Borrow> findByDueDate(java.time.LocalDate date);
}
