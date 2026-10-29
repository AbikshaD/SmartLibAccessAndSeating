package library_management.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import library_management.model.MaintenanceTeam;
import library_management.model.User;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByStudentId(String studentId );

    List<User> findByRoleOrderByNameAsc(String role);

    List<User> findByRoleAndMaintenanceTeamAndActiveTrueAndBlockedFalse(String role, MaintenanceTeam team);
}