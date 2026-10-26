package library_management.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import library_management.security.JwtService;
import library_management.repository.UserRepository;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
            UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.studentId(), request.password()));
        var principal = (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
        var user = userRepository.findByStudentId(principal.getUsername())
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(
                        "User not found: " + principal.getUsername()));
        return ResponseEntity.ok(new LoginResponse(jwtService.generateToken(principal),
                user.getRole(), user.getMaintenanceTeam()));
    }

    public record LoginRequest(@NotBlank String studentId, @NotBlank String password) { }

    public record LoginResponse(String token, String role, library_management.model.MaintenanceTeam team) { }
}