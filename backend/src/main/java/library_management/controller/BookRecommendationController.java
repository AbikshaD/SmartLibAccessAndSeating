package library_management.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import library_management.dto.BookRecommendationRequest;
import library_management.service.BookRecommendationService;
import library_management.service.BookRecommendationService.BookRecommendationResponse;

@RestController
@RequestMapping("/books/recommendations")
public class BookRecommendationController {

    private final BookRecommendationService recommendationService;

    public BookRecommendationController(BookRecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping("/chat")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<BookRecommendationResponse> chat(
            @Valid @RequestBody BookRecommendationRequest request) {
        return ResponseEntity.ok(recommendationService.recommend(request));
    }
}
