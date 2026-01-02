package com.graduate.novel.controller;

import com.graduate.novel.domain.user.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "User profile and password management endpoints")
public class UserController {

    private final UserService userService;

    /**
     * Get current user profile
     */
    @GetMapping("/profile")
    public ResponseEntity<UserDto> getProfile(@AuthenticationPrincipal User currentUser) {
        log.info("User {} requesting their profile", currentUser.getEmail());
        UserDto profile = userService.getProfile(currentUser);
        return ResponseEntity.ok(profile);
    }

    /**
     * Update current user profile
     */
    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        log.info("User {} updating their profile", currentUser.getEmail());
        UserDto updatedProfile = userService.updateProfile(currentUser, request);
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * Change password
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("User {} changing password", currentUser.getEmail());
        userService.changePassword(currentUser, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    /**
     * Forgot password - request password reset
     */
    @PostMapping("/forgot-password")
    @Operation(
        summary = "Request password reset",
        description = "Send password reset email to user. " +
                     "⚠️ **Email Testing Mode**: In Resend testing mode, emails only sent to verified addresses. " +
                     "Check application logs for email delivery status. " +
                     "To send to any email, verify a domain at https://resend.com/domains"
    )
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Forgot password request for email: {}", request.email());
        userService.forgotPassword(request);
        return ResponseEntity.ok(Map.of(
                "message", "If an account with that email exists, a password reset link has been sent"
        ));
    }

    /**
     * Reset password using token
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset password request with token");
        userService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    /**
     * Upload or update user avatar
     */
    @PostMapping("/avatar")
    public ResponseEntity<UserDto> uploadAvatar(
            @AuthenticationPrincipal User currentUser,
            @RequestParam("file") MultipartFile file) {
        log.info("User {} uploading avatar", currentUser.getEmail());
        UserDto updatedProfile = userService.uploadAvatar(currentUser, file);
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * Delete user avatar
     */
    @DeleteMapping("/avatar")
    public ResponseEntity<UserDto> deleteAvatar(@AuthenticationPrincipal User currentUser) {
        log.info("User {} deleting avatar", currentUser.getEmail());
        UserDto updatedProfile = userService.deleteAvatar(currentUser);
        return ResponseEntity.ok(updatedProfile);
    }
}

