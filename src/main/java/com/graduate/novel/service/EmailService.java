package com.graduate.novel.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from}")
    private String fromEmail;

    @Value("${app.url:http://localhost:8080}")
    private String appUrl;

    @Async
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
        }
    }

    @Async
    public void sendWelcomeEmail(String to, String displayName) {
        String subject = "Welcome to Novel Platform!";
        String body = String.format(
                "Hello %s,\n\n" +
                "Welcome to Novel Platform! Your account has been successfully created.\n\n" +
                "You can now log in and start exploring our collection of novels.\n\n" +
                "Best regards,\n" +
                "Novel Platform Team",
                displayName
        );
        sendEmail(to, subject, body);
    }

    @Async
    public void sendPasswordResetEmail(String to, String displayName, String resetToken) {
        String subject = "Password Reset Request";
        String resetLink = appUrl + "/reset-password?token=" + resetToken;
        String body = String.format(
                "Hello %s,\n\n" +
                "We received a request to reset your password. Click the link below to reset your password:\n\n" +
                "%s\n\n" +
                "This link will expire in 1 hour.\n\n" +
                "If you did not request a password reset, please ignore this email.\n\n" +
                "Best regards,\n" +
                "Novel Platform Team",
                displayName,
                resetLink
        );
        sendEmail(to, subject, body);
    }

    @Async
    public void sendPasswordChangedEmail(String to, String displayName) {
        String subject = "Password Changed Successfully";
        String body = String.format(
                "Hello %s,\n\n" +
                "Your password has been changed successfully.\n\n" +
                "If you did not make this change, please contact support immediately.\n\n" +
                "Best regards,\n" +
                "Novel Platform Team",
                displayName
        );
        sendEmail(to, subject, body);
    }

    @Async
    public void sendProfileUpdatedEmail(String to, String displayName) {
        String subject = "Profile Updated Successfully";
        String body = String.format(
                "Hello %s,\n\n" +
                "Your profile has been updated successfully.\n\n" +
                "If you did not make this change, please contact support immediately.\n\n" +
                "Best regards,\n" +
                "Novel Platform Team",
                displayName
        );
        sendEmail(to, subject, body);
    }

    @Async
    public void sendPasswordResetByAdminEmail(String to, String displayName, String temporaryPassword) {
        String subject = "Your Password Has Been Reset by Administrator";
        String body = String.format(
                "Hello %s,\n\n" +
                "Your password has been reset by an administrator.\n\n" +
                "Your temporary password is: %s\n\n" +
                "Please log in and change your password immediately.\n\n" +
                "Best regards,\n" +
                "Novel Platform Team",
                displayName,
                temporaryPassword
        );
        sendEmail(to, subject, body);
    }
}

