package library_management.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import library_management.exception.FeedbackNotFoundException;
import library_management.exception.UnauthorizedFeedbackAccessException;
import library_management.model.Feedback;
import library_management.repository.FeedbackRepository;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    public Feedback submit(String studentId, SubmitFeedbackRequest request) {
        Feedback feedback = new Feedback();
        feedback.setStudentId(studentId);
        feedback.setCategory(request.category());
        feedback.setRating(request.rating());
        feedback.setComment(request.comment().trim());
        feedback.setStatus(Feedback.Status.PENDING);
        feedback.setCreatedAt(LocalDateTime.now());
        return feedbackRepository.save(feedback);
    }

    public List<Feedback> getMyFeedback(String studentId) {
        return feedbackRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
    }

    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc();
    }

    public Feedback getFeedback(String feedbackId, String requesterId, boolean admin) {
        Feedback feedback = findFeedback(feedbackId);
        ensureCanAccess(feedback, requesterId, admin);
        return feedback;
    }

    public Feedback respond(String feedbackId, String response) {
        Feedback feedback = findFeedback(feedbackId);
        feedback.setAdminResponse(response.trim());
        feedback.setStatus(Feedback.Status.REVIEWED);
        return feedbackRepository.save(feedback);
    }

    public Feedback resolve(String feedbackId) {
        Feedback feedback = findFeedback(feedbackId);
        feedback.setStatus(Feedback.Status.RESOLVED);
        return feedbackRepository.save(feedback);
    }

    public void delete(String feedbackId, String requesterId, boolean admin) {
        Feedback feedback = findFeedback(feedbackId);
        ensureCanAccess(feedback, requesterId, admin);
        feedbackRepository.delete(feedback);
    }

    private Feedback findFeedback(String feedbackId) {
        return feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new FeedbackNotFoundException(feedbackId));
    }

    private void ensureCanAccess(Feedback feedback, String requesterId, boolean admin) {
        if (!admin && !feedback.getStudentId().equals(requesterId)) {
            throw new UnauthorizedFeedbackAccessException();
        }
    }

    public record SubmitFeedbackRequest(Feedback.Category category, int rating, String comment) { }
}