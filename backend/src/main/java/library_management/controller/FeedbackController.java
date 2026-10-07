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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import library_management.model.Feedback;
import library_management.service.FeedbackService;

@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Feedback> submit(@Valid @RequestBody SubmitFeedbackRequest request,
            Authentication authentication) {
        Feedback feedback = feedbackService.submit(authentication.getName(),
                new FeedbackService.SubmitFeedbackRequest(request.category(), request.rating(), request.comment()));
        return ResponseEntity.created(URI.create("/feedback/" + feedback.getId())).body(feedback);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<Feedback>> getMyFeedback(Authentication authentication) {
        return ResponseEntity.ok(feedbackService.getMyFeedback(authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Feedback>> getAllFeedback() {
        return ResponseEntity.ok(feedbackService.getAllFeedback());
    }

    @GetMapping("/{feedbackId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<Feedback> getFeedback(@PathVariable String feedbackId, Authentication authentication) {
        return ResponseEntity.ok(feedbackService.getFeedback(feedbackId, authentication.getName(), isAdmin(authentication)));
    }

    @DeleteMapping("/{feedbackId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String feedbackId, Authentication authentication) {
        feedbackService.delete(feedbackId, authentication.getName(), isAdmin(authentication));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{feedbackId}/respond")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Feedback> respond(@PathVariable String feedbackId,
            @Valid @RequestBody RespondRequest request) {
        return ResponseEntity.ok(feedbackService.respond(feedbackId, request.response()));
    }

    @PutMapping("/{feedbackId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Feedback> resolve(@PathVariable String feedbackId) {
        return ResponseEntity.ok(feedbackService.resolve(feedbackId));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    public record SubmitFeedbackRequest(
            @NotNull Feedback.Category category,
            @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(5) int rating,
            @NotBlank @Size(max = 2000) String comment) { }

    public record RespondRequest(@NotBlank @Size(max = 2000) String response) { }
}