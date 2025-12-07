package com.graduate.novel.domain.role;

import com.graduate.novel.common.exception.BadRequestException;
import com.graduate.novel.common.exception.ResourceNotFoundException;
import com.graduate.novel.common.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Transactional(readOnly = true)
    public List<RoleDto> getAllRoles() {
        log.debug("Fetching all roles");
        return roleRepository.findAll().stream()
                .map(roleMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleDto getRoleById(Long id) {
        log.debug("Fetching role by id: {}", id);
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
        return roleMapper.toDto(role);
    }

    @Transactional(readOnly = true)
    public RoleDto getRoleByName(String name) {
        log.debug("Fetching role by name: {}", name);
        Role role = roleRepository.findByName(name.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with name: " + name));
        return roleMapper.toDto(role);
    }

    @Transactional(readOnly = true)
    public Role findByName(String name) {
        return roleRepository.findByName(name.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with name: " + name));
    }

    @Transactional
    public RoleDto createRole(CreateRoleRequest request) {
        log.info("Creating new role: {}", request.name());

        String normalizedName = request.name().toUpperCase();
        if (roleRepository.existsByName(normalizedName)) {
            throw new BadRequestException("Role already exists with name: " + normalizedName);
        }

        Role role = Role.builder()
                .name(normalizedName)
                .description(request.description())
                .build();

        role = roleRepository.save(role);
        log.info("Created role with id: {}", role.getId());
        return roleMapper.toDto(role);
    }

    @Transactional
    public RoleDto updateRole(Long id, UpdateRoleRequest request) {
        log.info("Updating role with id: {}", id);

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        if (request.description() != null) {
            role.setDescription(request.description());
        }

        role = roleRepository.save(role);
        log.info("Updated role with id: {}", role.getId());
        return roleMapper.toDto(role);
    }

    @Transactional
    public void deleteRole(Long id) {
        log.info("Deleting role with id: {}", id);

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        // Prevent deletion of default roles
        if (Role.ADMIN.equals(role.getName()) ||
            Role.USER.equals(role.getName()) ||
            Role.MODERATOR.equals(role.getName())) {
            throw new BadRequestException("Cannot delete default role: " + role.getName());
        }

        roleRepository.delete(role);
        log.info("Deleted role with id: {}", id);
    }

    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return roleRepository.existsByName(name.toUpperCase());
    }
}

