package library_management.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import library_management.dto.BookRecommendationRequest;
import library_management.dto.BookRecommendationResult;
import library_management.exception.BookRecommendationException;
import library_management.model.Book;
import library_management.repository.BookRepository;

@Service
public class BookRecommendationService {

    private static final String EMPTY_CATALOG_MESSAGE =
            "There are no books in the library catalog to recommend yet.";
    private static final String NO_MATCH_MESSAGE =
            "I couldn't find a matching book in the current library catalog. Try another subject, genre, or author.";
    private static final String RESULTS_MESSAGE =
            "Here are books from the library catalog that match your request.";

    private final BookRepository bookRepository;
    private final GroqAIService groqAIService;

    public BookRecommendationService(BookRepository bookRepository, GroqAIService groqAIService) {
        this.bookRepository = bookRepository;
        this.groqAIService = groqAIService;
    }

    public BookRecommendationResponse recommend(BookRecommendationRequest request) {
        List<Book> catalog = bookRepository.findAllByOrderByTitleAsc();
        if (catalog.isEmpty()) {
            return new BookRecommendationResponse(EMPTY_CATALOG_MESSAGE, List.of());
        }

        List<BookRecommendationRequest.ConversationMessage> conversation =
                request.conversation() == null ? List.of() : request.conversation();
        Optional<BookRecommendationResult> result =
                groqAIService.recommendBooks(request.message().trim(), conversation, catalog);
        if (result.isEmpty()) {
            throw new BookRecommendationException(HttpStatus.BAD_GATEWAY,
                    "The recommendation service returned an invalid response. Please try again.");
        }

        Map<String, Book> booksById = catalog.stream()
                .filter(book -> book.getId() != null)
                .collect(Collectors.toMap(Book::getId, book -> book, (first, ignored) -> first,
                        LinkedHashMap::new));
        Set<String> seenIds = new HashSet<>();
        List<Book> recommendations = new ArrayList<>();
        for (String bookId : result.get().bookIds()) {
            Book book = booksById.get(bookId);
            if (book != null && seenIds.add(bookId)) {
                recommendations.add(book);
                if (recommendations.size() == 5) {
                    break;
                }
            }
        }

        if (recommendations.isEmpty()) {
            return new BookRecommendationResponse(NO_MATCH_MESSAGE, List.of());
        }
        return new BookRecommendationResponse(RESULTS_MESSAGE, recommendations);
    }

    public record BookRecommendationResponse(String message, List<Book> books) { }
}
