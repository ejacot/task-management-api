package com.ejacot.taskmanagement.auth;

import com.ejacot.taskmanagement.user.UserAccount;
import com.ejacot.taskmanagement.user.UserAccountRepository;
import com.ejacot.taskmanagement.hotel.DeliveryService;
import com.ejacot.taskmanagement.hotel.Hotel;
import com.ejacot.taskmanagement.hotel.HotelRepository;
import com.ejacot.taskmanagement.hotel.PayRate;
import com.ejacot.taskmanagement.hotel.PayRateRepository;
import com.ejacot.taskmanagement.hotel.UserRole;
import com.ejacot.taskmanagement.hotel.WorkType;
import com.ejacot.taskmanagement.hotel.WorkTypeRepository;
import com.ejacot.taskmanagement.hotel.WorkUnit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserAccountRepository users;
    private final HotelRepository hotels;
    private final WorkTypeRepository workTypes;
    private final PayRateRepository payRates;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final DeliveryService delivery;
    private final SecureRandom random = new SecureRandom();

    public AuthController(UserAccountRepository users, HotelRepository hotels, WorkTypeRepository workTypes,
                          PayRateRepository payRates, PasswordEncoder passwordEncoder,
                          TokenService tokenService, DeliveryService delivery) {
        this.users = users;
        this.hotels = hotels;
        this.workTypes = workTypes;
        this.payRates = payRates;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.delivery = delivery;
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
            throw new ResponseStatusException(HttpStatus.LOCKED, "Prea multe incercari gresite. Incearca din nou mai tarziu.");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            user.registerLoginFailure();
            users.save(user);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Contul sau parola nu sunt corecte");
        }
        user.registerLoginSuccess();
        users.save(user);
        return new LoginResponse(tokenService.create(user), user.getUsername(), user.getRole().name());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        String username = request.username().trim().toLowerCase();
        String email = request.email().trim().toLowerCase();
        if (users.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username existent");
        }
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email existent");
        }
        Hotel hotel = hotels.save(new Hotel(blank(request.hotelName(), "Hotelul meu"), blank(request.city(), "Oraș")));
        seedDefaultWorkTypes(hotel);
        UserAccount saved = new UserAccount(username, passwordEncoder.encode(request.password()), email, null,
                UserRole.EMPLOYER, BigDecimal.ZERO, hotel);
        saved.configureProfile(blank(request.firstName(), null), blank(request.lastName(), null), null, 1);
        String code = code();
        saved.requestEmailVerification(code, Instant.now().plus(Duration.ofMinutes(30)));
        users.save(saved);
        payRates.save(new PayRate(saved, BigDecimal.ZERO, LocalDate.now()));
        delivery.queueEmail(saved, "Cod confirmare Roomly", "Codul tau de confirmare este: " + code + ". Expira in 30 minute.");
        return new RegisterResponse("Cont creat. Confirmă codul primit pe email.", code, saved.getEmailVerificationExpiresAt());
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

    @GetMapping("/invitations/{token}")
    public InvitationResponse invitation(@PathVariable String token) {
        UserAccount user = users.findByInvitationToken(token)
                .filter(value -> value.getInvitationExpiresAt() != null && value.getInvitationExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitația nu mai este validă"));
        return new InvitationResponse(user.getUsername(), user.getEmail(), user.getPhone(), user.getInvitationExpiresAt());
    }

    @PostMapping("/invitations/{token}/accept")
    public UserResponse acceptInvitation(@PathVariable String token, @Valid @RequestBody AcceptInvitationRequest request) {
        UserAccount user = users.findByInvitationToken(token)
                .filter(value -> value.getInvitationExpiresAt() != null && value.getInvitationExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitația nu mai este validă"));
        user.acceptInvitation(passwordEncoder.encode(request.password()));
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
        delivery.queueEmail(user, "Cod resetare Roomly", "Codul tau de resetare este: " + code + ". Expira in 30 minute.");
        delivery.queueSms(user, "Roomly: cod resetare " + code);
        return new ResetRequestResponse("Codul de resetare a fost generat si pus in coada de trimitere.", code, user.getPasswordResetExpiresAt());
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
            @NotBlank @Size(min = 3, max = 50)
            @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "must contain only letters, numbers, dots, dashes or underscores")
            String username,
            @NotBlank @Email @Size(max = 150) String email,
            @Size(max = 80) String firstName,
            @Size(max = 80) String lastName,
            @Size(max = 120) String hotelName,
            @Size(max = 120) String city,
            @NotBlank @Size(min = 8, max = 72) String password) {}

    public record AcceptInvitationRequest(@NotBlank @Size(min = 8, max = 72) String password) {}
    public record LoginRequest(@NotBlank String login,@NotBlank String password) {}
    public record LoginResponse(String token,String username,String role) {}
    public record ResetRequest(@NotBlank String login) {}
    public record ResetConfirm(@NotBlank String code,@NotBlank @Size(min = 8, max = 72) String newPassword) {}
    public record ConfirmRegistrationRequest(@NotBlank String code) {}
    public record RegisterResponse(String message,String demoCode,Instant expiresAt) {}
    public record UserResponse(Long id, String username, Instant createdAt) {}
    public record InvitationResponse(String username,String email,String phone,Instant expiresAt) {}
    public record ResetRequestResponse(String message,String demoCode,Instant expiresAt) {}

    private String code() {
        return String.valueOf(100000 + random.nextInt(900000));
    }

    private String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void seedDefaultWorkTypes(Hotel hotel) {
        workTypes.save(new WorkType(hotel, "ROOM_NORMAL", "Camere Normal", WorkUnit.ROOMS, new BigDecimal("2.40"), "#17806D"));
        workTypes.save(new WorkType(hotel, "ROOM_JUNIOR", "Junior Suite", WorkUnit.ROOMS, new BigDecimal("1.60"), "#B67A2D"));
        workTypes.save(new WorkType(hotel, "ROOM_PRESIDENT", "President Suite", WorkUnit.ROOMS, new BigDecimal("1.20"), "#7D5BA6"));
        WorkType publicArea = workTypes.save(new WorkType(hotel, "PUBLIC", "Public Area", WorkUnit.HOURLY, null, "#3B6EA8"));
        publicArea.configureDefaults(LocalTime.of(5, 0), LocalTime.of(13, 30), 30);
        workTypes.save(new WorkType(hotel, "HSK", "Housekeeping", WorkUnit.HOURLY, null, "#D05D4E"));
    }
}
