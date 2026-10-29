package library_management.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import library_management.model.MaintenanceNotification;

public interface MaintenanceNotificationRepository extends MongoRepository<MaintenanceNotification, String> {

    List<MaintenanceNotification> findByRecipientIdOrderByCreatedAtDesc(String recipientId);

    long countByRecipientIdAndReadFalse(String recipientId);

    Optional<MaintenanceNotification> findByIdAndRecipientId(String id, String recipientId);
}
