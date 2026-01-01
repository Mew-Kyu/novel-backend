package com.graduate.novel.domain.user;

import com.graduate.novel.common.exception.BadRequestException;
import com.graduate.novel.common.exception.ResourceNotFoundException;
import com.graduate.novel.common.mapper.UserMapper;
import com.graduate.novel.service.CloudinaryService;
import com.graduate.novel.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final CloudinaryService cloudinaryService;

    private static final int RESET_TOKEN_EXPIRY_HOURS = 1;
    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Get current user profile
     */
    @Transactional(readOnly = true)
    public UserDto getProfile(User currentUser) {
        log.info("Getting profile for user: {}", currentUser.getEmail());
        return userMapper.toDto(currentUser);
    }

    /**
     * Update user profile
     */
    @Transactional
    public UserDto updateProfile(User currentUser, UpdateProfileRequest request) {
        log.info("Updating profile for user: {}", currentUser.getEmail());

        currentUser.setDisplayName(request.displayName());
        User updatedUser = userRepository.save(currentUser);

        // Send email notification
        emailService.sendProfileUpdatedEmail(updatedUser.getEmail(), updatedUser.getDisplayName());

        log.info("Profile updated successfully for user: {}", currentUser.getEmail());
        return userMapper.toDto(updatedUser);
    }

    /**
     * Change password
     */
    @Transactional
    public void changePassword(User currentUser, ChangePasswordRequest request) {
        log.info("Changing password for user: {}", currentUser.getEmail());

        // Verify current password
        if (!passwordEncoder.matches(request.currentPassword(), currentUser.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        // Update password
        currentUser.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(currentUser);

        // Send email notification
        emailService.sendPasswordChangedEmail(currentUser.getEmail(), currentUser.getDisplayName());

        log.info("Password changed successfully for user: {}", currentUser.getEmail());
    }

    /**
     * Initiate forgot password process
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("Processing forgot password request for email: {}", request.email());

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.email()));

        // Delete any existing tokens for this user
        passwordResetTokenRepository.deleteByUserId(user.getId());

        // Generate reset token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(RESET_TOKEN_EXPIRY_HOURS);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(expiryDate)
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);

        // Send email with reset link
        emailService.sendPasswordResetEmail(user.getEmail(), user.getDisplayName(), token);

        log.info("Password reset token generated for user: {}", user.getEmail());
    }

    /**
     * Reset password using token
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Processing password reset with token");

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (resetToken.getUsed()) {
            throw new BadRequestException("Reset token has already been used");
        }

        if (resetToken.isExpired()) {
            throw new BadRequestException("Reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Send email notification
        emailService.sendPasswordChangedEmail(user.getEmail(), user.getDisplayName());

        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    /**
     * Admin: Delete user account
     */
    @Transactional
    public void deleteUser(Long userId, User currentUser) {
        log.info("Admin {} deleting user: {}", currentUser.getEmail(), userId);

        // Prevent self-deletion
        if (currentUser.getId().equals(userId)) {
            throw new BadRequestException("Cannot delete your own account");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Delete any password reset tokens
        passwordResetTokenRepository.deleteByUserId(userId);

        userRepository.delete(user);
        log.info("User {} deleted successfully by admin {}", userId, currentUser.getEmail());
    }

    /**
     * Admin: Reset user password
     */
    @Transactional
    public String adminResetPassword(Long userId, AdminResetPasswordRequest request, User currentUser) {
        log.info("Admin {} resetting password for user: {}", currentUser.getEmail(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Generate temporary password if not provided
        String newPassword = request.newPassword();
        if (newPassword == null || newPassword.isBlank()) {
            newPassword = generateTemporaryPassword();
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Delete any existing reset tokens
        passwordResetTokenRepository.deleteByUserId(userId);

        // Send email notification
        emailService.sendPasswordResetByAdminEmail(user.getEmail(), user.getDisplayName(), newPassword);

        log.info("Password reset successfully for user {} by admin {}", userId, currentUser.getEmail());
        return newPassword;
    }

    /**
     * Generate a secure temporary password
     */
    private String generateTemporaryPassword() {
        byte[] randomBytes = new byte[12];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Cleanup expired reset tokens (should be scheduled)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Cleaning up expired password reset tokens");
        passwordResetTokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
    }

    /**
     * Upload or update user avatar
     */
    @Transactional
    public UserDto uploadAvatar(User currentUser, MultipartFile file) {
        log.info("Uploading avatar for user: {}", currentUser.getEmail());

        // Validate file
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Avatar file is required");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("File must be an image");
        }

        // Validate file size (max 5MB)
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new BadRequestException("Avatar file size must not exceed 5MB");
        }

        try {
            // Upload to Cloudinary
            String avatarUrl = cloudinaryService.uploadAvatar(file);

            // Update user avatar
            currentUser.setAvatarUrl(avatarUrl);
            User updatedUser = userRepository.save(currentUser);

            log.info("Avatar uploaded successfully for user: {}", currentUser.getEmail());
            return userMapper.toDto(updatedUser);
        } catch (IOException e) {
            log.error("Failed to upload avatar for user: {}", currentUser.getEmail(), e);
            throw new BadRequestException("Failed to upload avatar: " + e.getMessage());
        }
    }

    /**
     * Delete user avatar
     */
    @Transactional
    public UserDto deleteAvatar(User currentUser) {
        log.info("Deleting avatar for user: {}", currentUser.getEmail());

        if (currentUser.getAvatarUrl() == null || currentUser.getAvatarUrl().isBlank()) {
            throw new BadRequestException("User does not have an avatar to delete");
        }

        // Remove avatar URL
        currentUser.setAvatarUrl(null);
        User updatedUser = userRepository.save(currentUser);

        log.info("Avatar deleted successfully for user: {}", currentUser.getEmail());
        return userMapper.toDto(updatedUser);
    }
}

