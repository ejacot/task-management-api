package com.ejacot.taskmanagement;

import com.ejacot.taskmanagement.user.UserAccount;
import com.ejacot.taskmanagement.user.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "roomly.mail.enabled=true",
        "roomly.mail.from=test@roomly.local"
})
@AutoConfigureMockMvc
class AuthMailDeliveryIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @MockitoBean JavaMailSender mailSender;

    @Test
    void registerFailsCleanlyWhenConfirmationEmailCannotBeSent() throws Exception {
        reset(mailSender);
        doThrow(new MailSendException("SMTP timeout")).when(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"mail.failure@example.com","password":"strongpass1","confirmPassword":"strongpass1"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").exists());

        assertThat(users.existsByEmail("mail.failure@example.com")).isFalse();
    }

    @Test
    void passwordResetFailsCleanlyWhenResetEmailCannotBeSent() throws Exception {
        reset(mailSender);
        UserAccount user = new UserAccount(
                "reset.failure@example.com",
                passwordEncoder.encode("strongpass1"),
                "reset.failure@example.com",
                null,
                com.ejacot.taskmanagement.hotel.UserRole.EMPLOYEE,
                BigDecimal.ZERO,
                null
        );
        user.verifyEmail();
        users.save(user);

        doThrow(new MailSendException("SMTP timeout")).when(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));

        mvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"reset.failure@example.com"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").exists());

        UserAccount reloaded = users.findByLogin("reset.failure@example.com").orElseThrow();
        assertThat(reloaded.getPasswordResetCode()).isNull();
    }
}
