package library_management.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import library_management.model.Feedback;

public interface FeedbackRepository extends MongoRepository<Feedback, String> {
    List<Feedback> findByStudentIdOrderByCreatedAtDesc(String studentId);

    List<Feedback> findAllByOrderByCreatedAtDesc();
}