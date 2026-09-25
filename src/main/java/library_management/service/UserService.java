package library_management.service;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
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

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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