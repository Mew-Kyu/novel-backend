package com.graduate.novel.controller;

import com.graduate.novel.common.exception.BadRequestException;
import com.graduate.novel.common.exception.ResourceNotFoundException;
import com.graduate.novel.common.mapper.UserMapper;
import com.graduate.novel.domain.role.Role;
import com.graduate.novel.domain.role.RoleDto;
import com.graduate.novel.domain.role.RoleService;
import com.graduate.novel.domain.role.CreateRoleRequest;
import com.graduate.novel.domain.role.UpdateRoleRequest;
import com.graduate.novel.domain.user.AdminResetPasswordRequest;
import com.graduate.novel.domain.user.User;
import com.graduate.novel.domain.user.UserDto;
import com.graduate.novel.domain.user.UserRepository;
import com.graduate.novel.domain.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RoleService roleService;
    private final UserService userService;

    // ==================== USER MANAGEMENT ====================

    /**
     * List all users with their roles (paginated)
     */
    @GetMapping("/users")
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Admin fetching all users");
        Page<User> users = userRepository.findAll(pageable);
        Page<UserDto> userDtos = users.map(userMapper::toDto);
        return ResponseEntity.ok(userDtos);
    }

    /**
     * Get user by ID
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long userId) {
        log.info("Admin fetching user by id: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return ResponseEntity.ok(userMapper.toDto(user));
    }

    /**
     * Activate a user account
     */
    @PatchMapping("/users/{userId}/activate")
    public ResponseEntity<UserDto> activateUser(@PathVariable Long userId) {
        log.info("Admin activating user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setActive(true);
        user = userRepository.save(user);
        log.info("User {} activated successfully", userId);

        return ResponseEntity.ok(userMapper.toDto(user));
    }

    /**
     * Deactivate a user account
     */
    @PatchMapping("/users/{userId}/deactivate")
    public ResponseEntity<UserDto> deactivateUser(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long userId) {
        log.info("Admin deactivating user: {}", userId);

        // Prevent self-deactivation
        if (currentUser.getId().equals(userId)) {
            throw new BadRequestException("Cannot deactivate your own account");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setActive(false);
        user = userRepository.save(user);
        log.info("User {} deactivated successfully", userId);

        return ResponseEntity.ok(userMapper.toDto(user));
    }

    // ==================== SEARCH & FILTER ====================

    /**
     * Search users by email (contains)
     */
    @GetMapping("/users/search/email")
    public ResponseEntity<Page<UserDto>> searchByEmail(
            @RequestParam String email,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Admin searching users by email: {}", email);
        Page<User> users = userRepository.findByEmailContainingIgnoreCase(email, pageable);
        Page<UserDto> userDtos = users.map(userMapper::toDto);
        return ResponseEntity.ok(userDtos);
    }

    /**
     * Search users by display name (contains)
     */
    @GetMapping("/users/search/name")
    public ResponseEntity<Page<UserDto>> searchByDisplayName(
            @RequestParam String displayName,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Admin searching users by display name: {}", displayName);
        Page<User> users = userRepository.findByDisplayNameContainingIgnoreCase(displayName, pageable);
        Page<UserDto> userDtos = users.map(userMapper::toDto);
        return ResponseEntity.ok(userDtos);
    }

    /**
     * Filter users by active status
     */
    @GetMapping("/users/filter/status")
    public ResponseEntity<Page<UserDto>> filterByStatus(
            @RequestParam Boolean active,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Admin filtering users by status: {}", active);
        Page<User> users = userRepository.findByActive(active, pageable);
        Page<UserDto> userDtos = users.map(userMapper::toDto);
        return ResponseEntity.ok(userDtos);
    }

    /**
     * Search users by keyword (email or display name)
     */
    @GetMapping("/users/search/keyword")
    public ResponseEntity<Page<UserDto>> searchByKeyword(
            @RequestParam String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Admin searching users by keyword: {}", keyword);
        Page<User> users = userRepository.searchByKeyword(keyword, pageable);
        Page<UserDto> userDtos = users.map(userMapper::toDto);
        return ResponseEntity.ok(userDtos);
    }

    /**
     * Advanced search with multiple filters
     */
    @GetMapping("/users/search/advanced")
    public ResponseEntity<Page<UserDto>> advancedSearch(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Admin performing advanced search - email: {}, displayName: {}, active: {}", email, displayName, active);
        Page<User> users = userRepository.advancedSearch(email, displayName, active, pageable);
        Page<UserDto> userDtos = users.map(userMapper::toDto);
        return ResponseEntity.ok(userDtos);
    }

    // ==================== ROLE ASSIGNMENT ====================

    /**
     * Update a user's role
     */
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<UserDto> updateUserRole(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long userId,
            @RequestParam String roleName) {
        log.info("Admin updating role for user {} to {}", userId, roleName);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Role newRole = roleService.findByName(roleName);

        // Prevent removing ADMIN role from self
        if (currentUser.getId().equals(userId) &&
            user.hasRole(Role.ADMIN) &&
            !Role.ADMIN.equalsIgnoreCase(roleName)) {
            throw new BadRequestException("Cannot remove ADMIN role from your own account");
        }

        user.setRole(newRole);
        user = userRepository.save(user);
        log.info("Role updated to {} for user {} successfully", roleName, userId);

        return ResponseEntity.ok(userMapper.toDto(user));
    }

    // ==================== ROLE MANAGEMENT ====================

    /**
     * Get all roles
     */
    @GetMapping("/roles")
    public ResponseEntity<List<RoleDto>> getAllRoles() {
        log.info("Admin fetching all roles");
        List<RoleDto> roles = roleService.getAllRoles();
        return ResponseEntity.ok(roles);
    }

    /**
     * Get role by ID
     */
    @GetMapping("/roles/{roleId}")
    public ResponseEntity<RoleDto> getRoleById(@PathVariable Long roleId) {
        log.info("Admin fetching role by id: {}", roleId);
        RoleDto role = roleService.getRoleById(roleId);
        return ResponseEntity.ok(role);
    }

    /**
     * Create a new role
     */
    @PostMapping("/roles")
    public ResponseEntity<RoleDto> createRole(@Valid @RequestBody CreateRoleRequest request) {
        log.info("Admin creating new role: {}", request.name());
        RoleDto role = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(role);
    }

    /**
     * Update a role
     */
    @PutMapping("/roles/{roleId}")
    public ResponseEntity<RoleDto> updateRole(
            @PathVariable Long roleId,
            @Valid @RequestBody UpdateRoleRequest request) {
        log.info("Admin updating role: {}", roleId);
        RoleDto role = roleService.updateRole(roleId, request);
        return ResponseEntity.ok(role);
    }

    /**
     * Delete a role
     */
    @DeleteMapping("/roles/{roleId}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long roleId) {
        log.info("Admin deleting role: {}", roleId);
        roleService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete a user account
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long userId) {
        log.info("Admin {} deleting user: {}", currentUser.getEmail(), userId);
        userService.deleteUser(userId, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reset user password (admin action)
     */
    @PostMapping("/users/{userId}/reset-password")
    public ResponseEntity<Map<String, String>> resetUserPassword(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long userId,
            @Valid @RequestBody AdminResetPasswordRequest request) {
        log.info("Admin {} resetting password for user: {}", currentUser.getEmail(), userId);
        String newPassword = userService.adminResetPassword(userId, request, currentUser);
        return ResponseEntity.ok(Map.of(
                "message", "Password reset successfully",
                "temporaryPassword", newPassword
        ));
    }

    // ==================== STATISTICS ====================

    /**
     * Get user statistics
     */
    @GetMapping("/stats/users")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        log.info("Admin fetching user statistics");

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByActive(true);
        long inactiveUsers = userRepository.countByActive(false);

        Map<String, Object> stats = Map.of(
                "totalUsers", totalUsers,
                "activeUsers", activeUsers,
                "inactiveUsers", inactiveUsers
        );

        return ResponseEntity.ok(stats);
    }
}

