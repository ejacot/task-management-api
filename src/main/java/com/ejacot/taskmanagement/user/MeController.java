package com.ejacot.taskmanagement.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/me")
public class MeController {
    private final UserAccountRepository users;
    private final UserProfileRepository profiles;
    private final PasswordEncoder passwordEncoder;

    public MeController(UserAccountRepository users, UserProfileRepository profiles, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.profiles = profiles;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/profile")
    public ProfileResponse profile(Authentication authentication) {
        UserAccount user = users.findByUsername(authentication.getName()).orElseThrow();
        UserProfile profile = profiles.findByUserId(user.getId()).orElseGet(() -> profiles.save(new UserProfile(user)));
        return ProfileResponse.from(user, profile);
    }

    @PutMapping("/profile")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateProfile(Authentication authentication, @Valid @RequestBody ProfileUpdate request) {
        UserAccount user = users.findByUsername(authentication.getName()).orElseThrow();
        UserProfile profile = profiles.findByUserId(user.getId()).orElseGet(() -> new UserProfile(user));
        profile.update(request.firstName().trim(), request.lastName().trim(), request.defaultHourlyRate(),
                request.currency().trim().toUpperCase(), request.defaultBreakMinutes(), request.language().trim().toLowerCase());
        user.syncProfileBasics(profile.getFirstName(), profile.getLastName(), profile.getDefaultHourlyRate());
        profiles.save(profile);
        users.save(user);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePassword(Authentication authentication, @Valid @RequestBody PasswordUpdate request) {
        UserAccount user = users.findByUsername(authentication.getName()).orElseThrow();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parola actuală nu este corectă");
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        users.save(user);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateAccount(Authentication authentication) {
        UserAccount user = users.findByUsername(authentication.getName()).orElseThrow();
        user.deactivate();
        users.save(user);
    }

    public record ProfileResponse(
            Long userId,
            String email,
            boolean emailVerified,
            String firstName,
            String lastName,
            BigDecimal defaultHourlyRate,
            String currency,
            int defaultBreakMinutes,
            String language,
            boolean onboardingComplete
    ) {
        static ProfileResponse from(UserAccount user, UserProfile profile) {
            return new ProfileResponse(
                    user.getId(),
                    user.getEmail(),
                    user.isEmailVerified(),
                    profile.getFirstName(),
                    profile.getLastName(),
                    profile.getDefaultHourlyRate(),
                    profile.getCurrency(),
                    profile.getDefaultBreakMinutes(),
                    profile.getLanguage(),
                    profile.isCompleted()
            );
        }
    }

    public record ProfileUpdate(
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @DecimalMin(value = "0.00") BigDecimal defaultHourlyRate,
            @NotBlank @Pattern(regexp = "^[A-Za-z]{3}$") String currency,
            @Min(0) @Max(180) int defaultBreakMinutes,
            @NotBlank @Pattern(regexp = "^(ro|de|en|RO|DE|EN)$") String language
    ) {}

    public record PasswordUpdate(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72) String newPassword
    ) {}
}
