package library_management.service;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import library_management.exception.DuplicateEmailException;
import library_management.exception.UserNotFoundException;
import library_management.model.User;
import library_management.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongoTemplate;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, MongoTemplate mongoTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mongoTemplate = mongoTemplate;
    }

    public User createUser(User user) {
        normalizeEmail(user);
        normalizeRole(user);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        ensureEmailIsAvailable(user.getEmail(), null);
        try {
            return userRepository.save(user);
        } catch (DuplicateKeyException exception) {
            throw new DuplicateEmailException(user.getEmail());
        }
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(String id) {
        return userRepository.findByStudentId(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public User updateUser(String id, User updatedUser) {
        User existingUser = userRepository.findByStudentId(id).orElseThrow(() -> new UserNotFoundException(id));

        normalizeEmail(updatedUser);
        ensureEmailIsAvailable(updatedUser.getEmail(), id);

        existingUser.setStudentId(updatedUser.getStudentId());
        existingUser.setName(updatedUser.getName());
        existingUser.setEmail(updatedUser.getEmail());
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
            existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }
        normalizeRole(updatedUser);
        existingUser.setRole(updatedUser.getRole());

        try {
            return userRepository.save(existingUser);
        } catch (DuplicateKeyException exception) {
            throw new DuplicateEmailException(updatedUser.getEmail());
        }
    }

    public User unblockUser(String studentId) {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new UserNotFoundException(studentId));
        user.setBlocked(false);
        user.setBlockedReason(null);
        user.setBookingCancellationCount(0);
        return userRepository.save(user);
    }

        public void recordStudentBookingCancellation(String studentId) {
        User user = mongoTemplate.findAndModify(
            Query.query(Criteria.where("studentId").is(studentId)),
            new Update().inc("bookingCancellationCount", 1),
            FindAndModifyOptions.options().returnNew(true),
            User.class);
        if (user == null) {
            throw new UserNotFoundException(studentId);
        }

        if (user.getBookingCancellationCount() > 3 && !user.isBlocked()) {
            mongoTemplate.updateFirst(
                Query.query(Criteria.where("studentId").is(studentId)
                    .and("bookingCancellationCount").gt(3)),
                new Update().set("blocked", true)
                    .set("blockedReason", "Automatically blocked after more than 3 booking cancellations."),
                User.class);
        }
        }

    public User blockUser(String studentId, String reason) {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new UserNotFoundException(studentId));
        user.setBlocked(true);
        user.setBlockedReason(reason);
        return userRepository.save(user);
    }

    public void deleteUser(String id) {
        User user = userRepository.findByStudentId(id).orElseThrow(()-> new UserNotFoundException(id));
        userRepository.delete(user);
    }

    private void ensureEmailIsAvailable(String email, String currentUserId) {
        userRepository.findByEmailIgnoreCase(email)
                .filter(user -> !user.getId().equals(currentUserId))
                .ifPresent(user -> {
                    throw new DuplicateEmailException(email);
                });
    }

    private void normalizeEmail(User user) {
        user.setEmail(user.getEmail().trim().toLowerCase());
    }

    private void normalizeRole(User user) {
        String role = user.getRole();
        user.setRole(role == null || role.isBlank() ? "STUDENT" : role.trim().toUpperCase());
    }
}