package com.graduate.novel.controller;

import com.graduate.novel.common.exception.BadRequestException;
import com.graduate.novel.common.exception.ResourceNotFoundException;
import com.graduate.novel.common.mapper.UserMapper;
import com.graduate.novel.domain.role.Role;
import com.graduate.novel.domain.role.RoleDto;
import com.graduate.novel.domain.role.RoleService;
import com.graduate.novel.domain.role.CreateRoleRequest;
import com.graduate.novel.domain.role.UpdateRoleRequest;
import com.graduate.novel.domain.user.User;
import com.graduate.novel.domain.user.UserDto;
import com.graduate.novel.domain.user.UserRepository;
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

    // ==================== ROLE ASSIGNMENT ====================

    /**
     * Assign a role to a user
     */
    @PostMapping("/users/{userId}/roles/{roleName}")
    public ResponseEntity<UserDto> assignRoleToUser(
            @PathVariable Long userId,
            @PathVariable String roleName) {
        log.info("Admin assigning role {} to user {}", roleName, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Role role = roleService.findByName(roleName);

        if (user.hasRole(role.getName())) {
            throw new BadRequestException("User already has role: " + roleName);
        }

        user.addRole(role);
        user = userRepository.save(user);
        log.info("Role {} assigned to user {} successfully", roleName, userId);

        return ResponseEntity.ok(userMapper.toDto(user));
    }

    /**
     * Remove a role from a user
     */
    @DeleteMapping("/users/{userId}/roles/{roleName}")
    public ResponseEntity<UserDto> removeRoleFromUser(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long userId,
            @PathVariable String roleName) {
        log.info("Admin removing role {} from user {}", roleName, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Role role = roleService.findByName(roleName);

        // Prevent removing ADMIN role from self
        if (currentUser.getId().equals(userId) && Role.ADMIN.equalsIgnoreCase(roleName)) {
            throw new BadRequestException("Cannot remove ADMIN role from your own account");
        }

        if (!user.hasRole(role.getName())) {
            throw new BadRequestException("User does not have role: " + roleName);
        }

        // Ensure user has at least one role
        if (user.getRoles().size() <= 1) {
            throw new BadRequestException("Cannot remove the last role from user. User must have at least one role.");
        }

        user.removeRole(role);
        user = userRepository.save(user);
        log.info("Role {} removed from user {} successfully", roleName, userId);

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

