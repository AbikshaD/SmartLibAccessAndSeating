package library_management;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import library_management.model.Book;
import library_management.model.User;
import library_management.repository.BookRepository;
import library_management.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class BookManagementTests {

    private static final String TEST_DATABASE = "book_tests_"
            + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void configureMongoDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.database", () -> TEST_DATABASE);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminId;
    private String studentId;
    private String adminToken;
    private String studentToken;

    @BeforeEach
    void setUp() throws Exception {
        bookRepository.deleteAll();

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        adminId = "book-admin-" + suffix;
        studentId = "book-student-" + suffix;

        saveUser(adminId, "ADMIN");
        saveUser(studentId, "STUDENT");

        adminToken = login(adminId);
        studentToken = login(studentId);

        // Seed test books across 2 genres
        bookRepository.save(new Book("Clean Code", "Robert C. Martin", "Computer Science",
                "978-0132350884", 2008, 5, 4, "Floor 1 - Section CS-01", "Clean coding principles"));
        bookRepository.save(new Book("The Pragmatic Programmer", "David Thomas", "Computer Science",
                "978-0135957059", 2019, 3, 2, "Floor 1 - Section CS-02", "Software craftsmanship"));
        bookRepository.save(new Book("1984", "George Orwell", "Fiction",
                "978-0451524935", 1949, 4, 3, "Floor 2 - Section FIC-01", "Dystopian novel"));
    }

    @AfterEach
    void tearDown() {
        bookRepository.deleteAll();
        userRepository.findByStudentId(adminId).ifPresent(userRepository::delete);
        userRepository.findByStudentId(studentId).ifPresent(userRepository::delete);
    }

    private void saveUser(String id, String role) {
        User user = new User();
        user.setStudentId(id);
        user.setName(id);
        user.setEmail(id + "@example.com");
        user.setPassword(passwordEncoder.encode("book-test-password"));
        user.setRole(role);
        userRepository.save(user);
    }

    private String login(String id) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"" + id + "\",\"password\":\"book-test-password\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return response.replaceFirst(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }

    @Test
    void studentCanGetGenresWithCounts() throws Exception {
        mockMvc.perform(get("/books/genres")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void studentCanGetBooksByGenreAndCount() throws Exception {
        mockMvc.perform(get("/books/genre/Computer Science")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genre").value("Computer Science"))
                .andExpect(jsonPath("$.bookCount").value(2))
                .andExpect(jsonPath("$.books.length()").value(2));

        mockMvc.perform(get("/books/genre/Fiction")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genre").value("Fiction"))
                .andExpect(jsonPath("$.bookCount").value(1))
                .andExpect(jsonPath("$.books.length()").value(1));
    }

    @Test
    void studentCanFilterBooksByQueryParam() throws Exception {
        mockMvc.perform(get("/books?genre=Fiction")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("1984"));
    }
}
