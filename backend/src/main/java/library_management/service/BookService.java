package library_management.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import library_management.exception.BookNotFoundException;
import library_management.model.Book;
import library_management.repository.BookRepository;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAllByOrderByTitleAsc();
    }

    public List<Book> getBooksByGenre(String genre) {
        if (genre == null || genre.trim().isEmpty()) {
            return getAllBooks();
        }
        return bookRepository.findByGenreIgnoreCaseOrderByTitleAsc(genre.trim());
    }

    public long getBookCountByGenre(String genre) {
        if (genre == null || genre.trim().isEmpty()) {
            return bookRepository.count();
        }
        return bookRepository.countByGenreIgnoreCase(genre.trim());
    }

    public Book getBookById(String id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    public Book createBook(Book book) {
        if (book.getAvailableCopies() == null) {
            book.setAvailableCopies(book.getTotalCopies() != null ? book.getTotalCopies() : 1);
        }
        return bookRepository.save(book);
    }

    public Book updateBook(String id, Book updatedBook) {
        Book existing = getBookById(id);
        existing.setTitle(updatedBook.getTitle());
        existing.setAuthor(updatedBook.getAuthor());
        existing.setGenre(updatedBook.getGenre());
        existing.setIsbn(updatedBook.getIsbn());
        existing.setPublicationYear(updatedBook.getPublicationYear());
        existing.setTotalCopies(updatedBook.getTotalCopies());
        existing.setAvailableCopies(updatedBook.getAvailableCopies());
        existing.setShelfLocation(updatedBook.getShelfLocation());
        existing.setDescription(updatedBook.getDescription());
        return bookRepository.save(existing);
    }

    public void deleteBook(String id) {
        Book existing = getBookById(id);
        bookRepository.delete(existing);
    }

    public List<GenreSummary> getGenresWithCounts() {
        List<Book> allBooks = bookRepository.findAll();
        Map<String, GenreStatsAccumulator> stats = new LinkedHashMap<>();

        for (Book b : allBooks) {
            String g = b.getGenre() != null ? b.getGenre().trim() : "Uncategorized";
            stats.putIfAbsent(g, new GenreStatsAccumulator());
            GenreStatsAccumulator acc = stats.get(g);
            acc.bookCount++;
            acc.availableCopies += (b.getAvailableCopies() != null ? b.getAvailableCopies() : 0);
            acc.totalCopies += (b.getTotalCopies() != null ? b.getTotalCopies() : 0);
        }

        List<GenreSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, GenreStatsAccumulator> entry : stats.entrySet()) {
            summaries.add(new GenreSummary(
                    entry.getKey(),
                    entry.getValue().bookCount,
                    entry.getValue().availableCopies,
                    entry.getValue().totalCopies
            ));
        }

        return summaries;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedInitialBooksIfEmpty() {
        try {
            if (bookRepository.count() == 0) {
                List<Book> initialBooks = List.of(
                    // Computer Science
                    new Book("Clean Code: A Handbook of Agile Software Craftsmanship", "Robert C. Martin",
                            "Computer Science", "978-0132350884", 2008, 5, 4, "Floor 1 - Section CS-01",
                            "A handbook of agile software craftsmanship with best practices for writing readable, maintainable code."),
                    new Book("Introduction to Algorithms (CLRS)", "Thomas H. Cormen, Charles E. Leiserson",
                            "Computer Science", "978-0262033848", 2009, 6, 3, "Floor 1 - Section CS-02",
                            "Comprehensive textbook covering algorithm analysis, graph theory, dynamic programming, and data structures."),
                    new Book("Designing Data-Intensive Applications", "Martin Kleppmann",
                            "Computer Science", "978-1449373320", 2017, 4, 2, "Floor 1 - Section CS-03",
                            "The definitive guide to distributed data systems, reliability, scalability, and maintainability."),
                    new Book("The Pragmatic Programmer", "David Thomas, Andrew Hunt",
                            "Computer Science", "978-0135957059", 2019, 4, 4, "Floor 1 - Section CS-04",
                            "Timeless journey on personal craftsmanship, professional software engineering, and clean architecture."),
                    new Book("Artificial Intelligence: A Modern Approach", "Stuart Russell, Peter Norvig",
                            "Computer Science", "978-0134610993", 2020, 5, 3, "Floor 1 - Section CS-05",
                            "Leading authority on AI theory, machine learning algorithms, probabilistic reasoning, and robotics."),

                    // Fiction
                    new Book("1984", "George Orwell",
                            "Fiction", "978-0451524935", 1949, 7, 5, "Floor 2 - Section FIC-01",
                            "Classic dystopian novel depicting state surveillance, totalitarianism, and individual resistance."),
                    new Book("To Kill a Mockingbird", "Harper Lee",
                            "Fiction", "978-0060935467", 1960, 6, 4, "Floor 2 - Section FIC-02",
                            "Pulitzer Prize-winning tale of justice, compassion, and morality in the American South."),
                    new Book("The Great Gatsby", "F. Scott Fitzgerald",
                            "Fiction", "978-0743273565", 1925, 5, 3, "Floor 2 - Section FIC-03",
                            "Exploration of ambition, wealth, love, and illusion during the Roaring Twenties in America."),
                    new Book("Brave New World", "Aldous Huxley",
                            "Fiction", "978-0060850524", 1932, 4, 2, "Floor 2 - Section FIC-04",
                            "Visionary dystopian satire illustrating biological conditioning and consumerist control."),
                    new Book("The Alchemist", "Paulo Coelho",
                            "Fiction", "978-0062315007", 1988, 8, 6, "Floor 2 - Section FIC-05",
                            "Inspiring fable following Santiago, an Andalusian shepherd boy searching for his personal legend."),

                    // Science & Mathematics
                    new Book("A Brief History of Time", "Stephen Hawking",
                            "Science & Mathematics", "978-0553380163", 1988, 5, 3, "Floor 1 - Section SCI-01",
                            "Landmark overview of cosmology, black holes, the Big Bang, and the nature of space and time."),
                    new Book("Cosmos", "Carl Sagan",
                            "Science & Mathematics", "978-0345539434", 1980, 4, 3, "Floor 1 - Section SCI-02",
                            "Celebrated exploration of the universe, astronomical history, and humanity's cosmic origins."),
                    new Book("Calculus: Early Transcendentals", "James Stewart",
                            "Science & Mathematics", "978-1285741550", 2015, 6, 5, "Floor 1 - Section SCI-03",
                            "Master reference on limits, derivatives, integrals, and multivariable calculus applications."),
                    new Book("The Selfish Gene", "Richard Dawkins",
                            "Science & Mathematics", "978-0198788607", 1976, 4, 2, "Floor 1 - Section SCI-04",
                            "Foundational evolutionary biology work demonstrating the gene-centric perspective of evolution."),

                    // History & Philosophy
                    new Book("Sapiens: A Brief History of Humankind", "Yuval Noah Harari",
                            "History & Philosophy", "978-0062316097", 2015, 6, 4, "Floor 2 - Section HIS-01",
                            "Sweeping narrative exploring human evolution from hunter-gatherers to global society."),
                    new Book("Meditations", "Marcus Aurelius",
                            "History & Philosophy", "978-0812968255", 2002, 5, 4, "Floor 2 - Section HIS-02",
                            "Personal philosophical writings and timeless Stoic principles from the Roman Emperor."),
                    new Book("Guns, Germs, and Steel", "Jared Diamond",
                            "History & Philosophy", "978-0393354324", 1997, 4, 2, "Floor 2 - Section HIS-03",
                            "Pulitzer Prize-winning analysis of how geography shaped human societies and civilizations."),
                    new Book("The Republic", "Plato",
                            "History & Philosophy", "978-0140455113", 2007, 4, 3, "Floor 2 - Section HIS-04",
                            "Seminal philosophical dialogue analyzing justice, governance, and the ideal philosophical city."),

                    // Business & Management
                    new Book("The Lean Startup", "Eric Ries",
                            "Business & Management", "978-0307887894", 2011, 5, 4, "Floor 3 - Section BUS-01",
                            "Revolutionary framework for building businesses through continuous innovation and rapid iteration."),
                    new Book("Zero to One", "Peter Thiel, Blake Masters",
                            "Business & Management", "978-0770436774", 2014, 6, 5, "Floor 3 - Section BUS-02",
                            "Provocative guide on innovation, monopolistic technology advantages, and creating novel futures."),
                    new Book("Thinking, Fast and Slow", "Daniel Kahneman",
                            "Business & Management", "978-0374533557", 2011, 5, 3, "Floor 3 - Section BUS-03",
                            "Nobel Laureate's tour de force on cognitive biases, decision making, and mental heuristics."),
                    new Book("Principles: Life and Work", "Ray Dalio",
                            "Business & Management", "978-1501124020", 2017, 4, 3, "Floor 3 - Section BUS-04",
                            "Unconventional principles and systematic methods for radical truth and meaningful work.")
                );
                bookRepository.saveAll(initialBooks);
            }
        } catch (Exception e) {
            // Log but don't block startup
            System.err.println("Note: Could not automatically seed books (e.g. if MongoDB is not connected during build): " + e.getMessage());
        }
    }

    private static class GenreStatsAccumulator {
        long bookCount = 0;
        long availableCopies = 0;
        long totalCopies = 0;
    }

    public record GenreSummary(String genre, long bookCount, long availableCopies, long totalCopies) {}
}
