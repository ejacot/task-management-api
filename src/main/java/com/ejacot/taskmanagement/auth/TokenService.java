package com.ejacot.taskmanagement.auth;

import com.ejacot.taskmanagement.user.UserAccount;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class TokenService {
    private static final String SECRET = System.getenv().getOrDefault("ROOMLY_TOKEN_SECRET", "roomly-local-demo-secret-change-me");
    public String create(UserAccount user) {
        long expires = Instant.now().plusSeconds(60L * 60 * 24 * 14).getEpochSecond();
        String payload = user.getUsername() + ":" + expires;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + sign(payload);
    }
    public Optional<String> username(String token) {
        try {
            String[] parts = token.split("\\.", 2);
            if (parts.length != 2) return Optional.empty();
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            if (!sign(payload).equals(parts[1])) return Optional.empty();
            String[] values = payload.split(":", 2);
            if (values.length != 2 || Long.parseLong(values[1]) < Instant.now().getEpochSecond()) return Optional.empty();
            return Optional.of(values[0]);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
