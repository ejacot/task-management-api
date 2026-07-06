package com.ejacot.taskmanagement.auth;

import com.ejacot.taskmanagement.user.UserAccount;
import com.ejacot.taskmanagement.user.UserAccountRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserAccountRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        String username = request.username().trim().toLowerCase();
        if (users.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        UserAccount saved = users.save(new UserAccount(username, passwordEncoder.encode(request.password())));
        return new UserResponse(saved.getId(), saved.getUsername(), saved.getCreatedAt());
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50)
            @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "must contain only letters, numbers, dots, dashes or underscores")
            String username,
            @NotBlank @Size(min = 8, max = 72) String password) {}

    public record UserResponse(Long id, String username, Instant createdAt) {}
}

