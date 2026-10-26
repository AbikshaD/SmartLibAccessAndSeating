package library_management.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import library_management.model.MaintenanceTeam;
import library_management.model.MaintenanceTicket;

public interface MaintenanceTicketRepository extends MongoRepository<MaintenanceTicket, String> {
    List<MaintenanceTicket> findByTeamOrderBySubmittedAtDesc(MaintenanceTeam team);
}
