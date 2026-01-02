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

    @Async("taskExecutor")
    public void sendEmail(String to, String subject, String body) {
        try {
            log.info("Attempting to send email to: {} with subject: {}", to, subject);
            log.debug("Email configuration - From: {}, Host: resend", fromEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            String errorMsg = e.getMessage();

            // Check if it's a Resend testing mode restriction
            if (errorMsg != null && errorMsg.contains("You can only send testing emails to your own email address")) {
                log.warn("⚠️ Email not sent to: {} - Resend is in testing mode. To send to any email, verify a domain at resend.com/domains", to);
                log.info("💡 For development: You can only send emails to your verified email address in Resend");
            } else {
                log.error("Failed to send email to: {}. Error: {}", to, errorMsg, e);
            }

            // Don't throw exception for async methods - just log the error
            // This prevents cluttering logs with unnecessary stack traces
        }
    }

    @Async("taskExecutor")
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

    @Async("taskExecutor")
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

    @Async("taskExecutor")
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

    @Async("taskExecutor")
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

    @Async("taskExecutor")
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

