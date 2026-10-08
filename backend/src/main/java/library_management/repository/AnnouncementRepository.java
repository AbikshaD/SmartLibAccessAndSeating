package library_management.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import library_management.model.Announcement;

@Repository
public interface AnnouncementRepository extends MongoRepository<Announcement, String> {

    List<Announcement> findAllByOrderByCreatedAtDesc();

    List<Announcement> findByActiveTrueOrderByCreatedAtDesc();
}
