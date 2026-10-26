package library_management.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import library_management.model.User;
import library_management.model.MaintenanceTeam;
import library_management.service.UserService;

@RestController
@RequestMapping("/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping
	public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
		user.setRole("STUDENT");
		User createdUser = userService.createUser(user);
		return ResponseEntity.created(URI.create("/users/" + createdUser.getId())).body(createdUser);
	}

	@PostMapping("/admin")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<User> createAdmin(@Valid @RequestBody User user) {
		user.setRole("ADMIN");
		User createdUser = userService.createUser(user);
		return ResponseEntity.created(URI.create("/users/" + createdUser.getId())).body(createdUser);
	}

	@PostMapping("/maintenance-admin")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<User> createMaintenanceAdmin(@Valid @RequestBody User user) {
		user.setRole("MAINTENANCE_ADMIN");
		User createdUser = userService.createUser(user);
		return ResponseEntity.created(URI.create("/users/" + createdUser.getId())).body(createdUser);
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<User>> getAllUsers() {
		return ResponseEntity.ok(userService.getAllUsers());
	}

	@PostMapping("/maintenance")
	@PreAuthorize("hasRole('MAINTENANCE_ADMIN')")
	public ResponseEntity<User> createMaintenanceUser(@Valid @RequestBody CreateMaintenanceUserRequest request) {
		User createdUser = userService.createMaintenanceUser(request.studentId(), request.name(), request.email(),
				request.password(), request.team());
		return ResponseEntity.created(URI.create("/users/" + createdUser.getStudentId())).body(createdUser);
	}

	@GetMapping("/maintenance")
	@PreAuthorize("hasRole('MAINTENANCE_ADMIN')")
	public ResponseEntity<List<User>> getMaintenanceUsers() {
		return ResponseEntity.ok(userService.getMaintenanceUsers());
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'MAINTENANCE_ADMIN') or #id == authentication.name")
	public ResponseEntity<User> getUserById(@PathVariable String id) {
		return ResponseEntity.ok(userService.getUserById(id));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'MAINTENANCE_ADMIN') or #id == authentication.name")
	public ResponseEntity<User> updateUser(@PathVariable String id, @Valid @RequestBody User user) {
		return ResponseEntity.ok(userService.updateUser(id, user));
	}

	@PutMapping("/{id}/maintenance-team")
	@PreAuthorize("hasRole('MAINTENANCE_ADMIN')")
	public ResponseEntity<User> updateMaintenanceTeam(@PathVariable String id,
			@Valid @RequestBody MaintenanceTeamRequest request) {
		return ResponseEntity.ok(userService.updateMaintenanceTeam(id, request.team()));
	}

	@PutMapping("/{id}/active")
	@PreAuthorize("hasAnyRole('ADMIN', 'MAINTENANCE_ADMIN')")
	public ResponseEntity<User> updateMaintenanceAccountStatus(@PathVariable String id,
			@Valid @RequestBody AccountStatusRequest request, Authentication authentication) {
		boolean admin = authentication.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
		return ResponseEntity.ok(userService.updateAccountStatus(id, request.active(), admin));
	}

	@PutMapping("/{id}/unblock")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<User> unblockUser(@PathVariable String id) {
		return ResponseEntity.ok(userService.unblockUser(id));
	}

	@PutMapping("/{id}/enable")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<User> enableUser(@PathVariable String id) {
		return ResponseEntity.ok(userService.unblockUser(id));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deleteUser(@PathVariable String id) {
		userService.deleteUser(id);
		return ResponseEntity.noContent().build();
	}

	public record CreateMaintenanceUserRequest(
			@NotBlank String studentId,
			@NotBlank String name,
			@NotBlank @Email String email,
			@NotBlank @Size(min = 8, max = 100) String password,
			@NotNull MaintenanceTeam team) { }

	public record MaintenanceTeamRequest(@NotNull MaintenanceTeam team) { }

	public record AccountStatusRequest(@NotNull Boolean active) { }
}
