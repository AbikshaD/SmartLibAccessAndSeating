package library_management.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
                            "Unconventional principles and systematic methods for radical truth and meaningful work."),

                    // Additional Computer Science
                    new Book("Effective Java", "Joshua Bloch",
                            "Computer Science", null, 2018, 3, 3, "Floor 1 - Section CS-06",
                            "Practical guidance for writing robust, maintainable Java programs."),
                    new Book("Clean Architecture", "Robert C. Martin",
                            "Computer Science", null, 2017, 3, 3, "Floor 1 - Section CS-07",
                            "Principles for designing software systems that remain independent of frameworks and infrastructure."),
                    new Book("Head First Design Patterns", "Eric Freeman, Elisabeth Robson",
                            "Computer Science", null, 2020, 3, 3, "Floor 1 - Section CS-08",
                            "An illustrated introduction to reusable object-oriented design patterns."),
                    new Book("Computer Networking: A Top-Down Approach", "James F. Kurose, Keith W. Ross",
                            "Computer Science", null, 2021, 3, 3, "Floor 1 - Section CS-09",
                            "A layered introduction to networking concepts, protocols, and applications."),

                    // Additional Fiction
                    new Book("Pride and Prejudice", "Jane Austen",
                            "Fiction", null, 1813, 3, 3, "Floor 2 - Section FIC-06",
                            "A classic novel about family, social expectations, and first impressions."),
                    new Book("Frankenstein", "Mary Shelley",
                            "Fiction", null, 1818, 3, 3, "Floor 2 - Section FIC-07",
                            "A Gothic novel exploring scientific ambition, responsibility, and belonging."),
                    new Book("The Hobbit", "J. R. R. Tolkien",
                            "Fiction", null, 1937, 3, 3, "Floor 2 - Section FIC-08",
                            "A fantasy adventure following Bilbo Baggins on a journey beyond the Shire."),
                    new Book("The Kite Runner", "Khaled Hosseini",
                            "Fiction", null, 2003, 3, 3, "Floor 2 - Section FIC-09",
                            "A story of friendship, family, and redemption set across Afghanistan and the United States."),

                    // Additional Science & Mathematics
                    new Book("The Elegant Universe", "Brian Greene",
                            "Science & Mathematics", null, 1999, 3, 3, "Floor 1 - Section SCI-05",
                            "An accessible exploration of string theory and modern ideas in theoretical physics."),
                    new Book("Silent Spring", "Rachel Carson",
                            "Science & Mathematics", null, 1962, 3, 3, "Floor 1 - Section SCI-06",
                            "A landmark account of pesticide use and its effects on ecosystems."),
                    new Book("The Gene: An Intimate History", "Siddhartha Mukherjee",
                            "Science & Mathematics", null, 2016, 3, 3, "Floor 1 - Section SCI-07",
                            "A history of genetics and the science of heredity."),
                    new Book("Introduction to Linear Algebra", "Gilbert Strang",
                            "Science & Mathematics", null, 2016, 3, 3, "Floor 1 - Section SCI-08",
                            "A textbook covering vectors, matrices, linear transformations, and their applications."),

                    // Additional History & Philosophy
                    new Book("The Art of War", "Sun Tzu",
                            "History & Philosophy", null, null, 3, 3, "Floor 2 - Section HIS-05",
                            "An ancient Chinese treatise on strategy, planning, and conflict."),
                    new Book("The Second Sex", "Simone de Beauvoir",
                            "History & Philosophy", null, 1949, 3, 3, "Floor 2 - Section HIS-06",
                            "A foundational philosophical examination of women's social and historical circumstances."),
                    new Book("The Silk Roads: A New History of the World", "Peter Frankopan",
                            "History & Philosophy", null, 2015, 3, 3, "Floor 2 - Section HIS-07",
                            "A global history centered on the trade routes connecting Asia, Europe, and Africa."),
                    new Book("The Story of Philosophy", "Will Durant",
                            "History & Philosophy", null, 1926, 3, 3, "Floor 2 - Section HIS-08",
                            "An introduction to influential Western philosophers and their ideas."),

                    // Additional Business & Management
                    new Book("Good to Great", "Jim Collins",
                            "Business & Management", null, 2001, 3, 3, "Floor 3 - Section BUS-05",
                            "A study of organizational practices associated with sustained business performance."),
                    new Book("Start with Why", "Simon Sinek",
                            "Business & Management", null, 2009, 3, 3, "Floor 3 - Section BUS-06",
                            "A framework for communicating purpose and inspiring teams and customers."),
                    new Book("The Innovator's Dilemma", "Clayton M. Christensen",
                            "Business & Management", null, 1997, 3, 3, "Floor 3 - Section BUS-07",
                            "An analysis of disruptive innovation and the challenges it creates for established companies."),
                    new Book("Freakonomics", "Steven D. Levitt, Stephen J. Dubner",
                            "Business & Management", null, 2005, 3, 3, "Floor 3 - Section BUS-08",
                            "An accessible look at how economic reasoning can reveal unexpected patterns in everyday life."),

                    // Literature & Classics
                    new Book("Things Fall Apart", "Chinua Achebe",
                            "Literature & Classics", null, 1958, 3, 3, "Floor 2 - Section LIT-01",
                            "A novel about Igbo society and the effects of colonial rule in Nigeria."),
                    new Book("One Hundred Years of Solitude", "Gabriel García Márquez",
                            "Literature & Classics", null, 1967, 3, 3, "Floor 2 - Section LIT-02",
                            "A multigenerational story of the Buendía family and the town of Macondo."),
                    new Book("The Odyssey", "Homer",
                            "Literature & Classics", null, null, 3, 3, "Floor 2 - Section LIT-03",
                            "An ancient Greek epic following Odysseus on his long journey home."),
                    new Book("Crime and Punishment", "Fyodor Dostoevsky",
                            "Literature & Classics", null, 1866, 3, 3, "Floor 2 - Section LIT-04",
                            "A novel examining conscience, morality, and redemption in nineteenth-century St. Petersburg.")
            );

            Set<String> existingBooks = new HashSet<>();
            for (Book book : bookRepository.findAll()) {
                existingBooks.add(bookKey(book));
            }

            List<Book> missingBooks = initialBooks.stream()
                    .filter(book -> existingBooks.add(bookKey(book)))
                    .toList();
            if (!missingBooks.isEmpty()) {
                bookRepository.saveAll(missingBooks);
            }
        } catch (Exception e) {
            // Log but don't block startup
            System.err.println("Note: Could not automatically seed books (e.g. if MongoDB is not connected during build): " + e.getMessage());
        }
    }

    private String bookKey(Book book) {
        if (book.getIsbn() != null && !book.getIsbn().isBlank()) {
            return "isbn:" + book.getIsbn().replaceAll("[^0-9Xx]", "").toUpperCase(Locale.ROOT);
        }
        return "title-author:"
                + (book.getTitle() == null ? "" : book.getTitle().trim().toLowerCase(Locale.ROOT))
                + "|" + (book.getAuthor() == null ? "" : book.getAuthor().trim().toLowerCase(Locale.ROOT));
    }

    private static class GenreStatsAccumulator {
        long bookCount = 0;
        long availableCopies = 0;
        long totalCopies = 0;
    }

    public record GenreSummary(String genre, long bookCount, long availableCopies, long totalCopies) {}
}
