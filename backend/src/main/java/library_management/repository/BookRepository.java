package library_management.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import library_management.model.Book;

@Repository
public interface BookRepository extends MongoRepository<Book, String> {

    List<Book> findByGenreIgnoreCase(String genre);

    List<Book> findByGenreIgnoreCaseOrderByTitleAsc(String genre);

    long countByGenreIgnoreCase(String genre);

    List<Book> findAllByOrderByTitleAsc();
}
