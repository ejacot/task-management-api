package com.ejacot.taskmanagement.auth;

import com.ejacot.taskmanagement.hotel.DeliveryService;
import com.ejacot.taskmanagement.hotel.PayRate;
import com.ejacot.taskmanagement.hotel.PayRateRepository;
import com.ejacot.taskmanagement.hotel.UserRole;
import com.ejacot.taskmanagement.hotel.WorkType;
import com.ejacot.taskmanagement.hotel.WorkTypeRepository;
import com.ejacot.taskmanagement.hotel.WorkUnit;
import com.ejacot.taskmanagement.user.UserAccount;
import com.ejacot.taskmanagement.user.UserAccountRepository;
import com.ejacot.taskmanagement.user.UserProfile;
import com.ejacot.taskmanagement.user.UserProfileRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserAccountRepository users;
    private final UserProfileRepository profiles;
    private final WorkTypeRepository workTypes;
    private final PayRateRepository payRates;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final DeliveryService delivery;
    private final boolean mailEnabled;
    private final SecureRandom random = new SecureRandom();

    public AuthController(UserAccountRepository users,
                          UserProfileRepository profiles,
                          WorkTypeRepository workTypes,
                          PayRateRepository payRates,
                          PasswordEncoder passwordEncoder,
                          TokenService tokenService,
                          DeliveryService delivery,
                          @Value("${roomly.mail.enabled:false}") boolean mailEnabled) {
        this.users = users;
        this.profiles = profiles;
        this.workTypes = workTypes;
        this.payRates = payRates;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.delivery = delivery;
        this.mailEnabled = mailEnabled;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        UserAccount user = users.findByLogin(request.login())
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Contul sau parola nu sunt corecte"));
        if (!user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Confirmă emailul înainte de autentificare.");
        }
        if (user.isLocked()) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "Prea multe încercări greșite. Încearcă din nou mai târziu.");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            user.registerLoginFailure();
            users.save(user);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Contul sau parola nu sunt corecte");
        }
        user.registerLoginSuccess();
        users.save(user);
        boolean onboardingComplete = profiles.findByUserId(user.getId()).map(UserProfile::isCompleted).orElse(false);
        return new LoginResponse(tokenService.create(user), user.getUsername(), user.getRole().name(), onboardingComplete);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parolele nu coincid");
        }
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email existent");
        }
        UserAccount saved = new UserAccount(email, passwordEncoder.encode(request.password()), email, null,
                UserRole.EMPLOYEE, BigDecimal.ZERO, null);
        String code = code();
        saved.requestEmailVerification(code, Instant.now().plus(Duration.ofMinutes(30)));
        saved = users.save(saved);
        profiles.save(new UserProfile(saved));
        seedDefaultWorkTypes(saved);
        payRates.save(new PayRate(saved, BigDecimal.ZERO, LocalDate.now()));
        delivery.queueEmail(saved, "Cod confirmare Roomly", "Codul tău de confirmare este: " + code + ". Expiră în 30 minute.");
        return new RegisterResponse("Cont creat. Confirmă codul primit pe email.", mailEnabled ? null : code, saved.getEmailVerificationExpiresAt(), saved.getEmail());
    }

    @PostMapping("/register/confirm")
    public UserResponse confirmRegistration(@Valid @RequestBody ConfirmRegistrationRequest request) {
        UserAccount user = users.findByEmailVerificationCode(request.code())
                .filter(value -> value.getEmailVerificationExpiresAt() != null && value.getEmailVerificationExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cod invalid sau expirat"));
        user.verifyEmail();
        users.save(user);
        return new UserResponse(user.getId(), user.getUsername(), user.getCreatedAt());
    }

    @PostMapping("/password-reset/request")
    public ResetRequestResponse resetRequest(@Valid @RequestBody ResetRequest request) {
        UserAccount user = users.findByLogin(request.login())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contul nu există"));
        String code = code();
        user.requestPasswordReset(code, Instant.now().plus(Duration.ofMinutes(30)));
        users.save(user);
        delivery.queueEmail(user, "Cod resetare Roomly", "Codul tău de resetare este: " + code + ". Expiră în 30 minute.");
        delivery.queueSms(user, "Roomly: cod resetare " + code);
        return new ResetRequestResponse("Codul de resetare a fost trimis pe email.", mailEnabled ? null : code, user.getPasswordResetExpiresAt());
    }

    @PostMapping("/password-reset/confirm")
    public UserResponse resetConfirm(@Valid @RequestBody ResetConfirm request) {
        UserAccount user = users.findByPasswordResetCode(request.code())
                .filter(value -> value.getPasswordResetExpiresAt() != null && value.getPasswordResetExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cod invalid sau expirat"));
        user.resetPassword(passwordEncoder.encode(request.newPassword()));
        users.save(user);
        return new UserResponse(user.getId(), user.getUsername(), user.getCreatedAt());
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(min = 8, max = 72) String confirmPassword) {}

    public record LoginRequest(@NotBlank String login, @NotBlank String password) {}
    public record LoginResponse(String token, String username, String role, boolean onboardingComplete) {}
    public record ResetRequest(@NotBlank String login) {}
    public record ResetConfirm(@NotBlank String code, @NotBlank @Size(min = 8, max = 72) String newPassword) {}
    public record ConfirmRegistrationRequest(@NotBlank String code) {}
    public record RegisterResponse(String message, String demoCode, Instant expiresAt, String email) {}
    public record UserResponse(Long id, String username, Instant createdAt) {}
    public record ResetRequestResponse(String message, String demoCode, Instant expiresAt) {}

    private String code() {
        return String.valueOf(100000 + random.nextInt(900000));
    }

    private void seedDefaultWorkTypes(UserAccount user) {
        workTypes.save(new WorkType(user, "CHECK", "Checker / CH", WorkUnit.HOURLY, null, "#3B82F6"));
        workTypes.save(new WorkType(user, "PUBLIC", "Public Area", WorkUnit.HOURLY, null, "#14B8A6"));
        workTypes.save(new WorkType(user, "HOUSEKEEPING", "Housekeeping", WorkUnit.HOURLY, null, "#8B5CF6"));
        workTypes.save(new WorkType(user, "OBJ", "Objektleitung", WorkUnit.HOURLY, null, "#F59E0B"));
        workTypes.save(new WorkType(user, "OTHER", "Altă activitate", WorkUnit.HOURLY, null, "#64748B"));
    }
}
