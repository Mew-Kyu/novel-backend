package com.graduate.novel.domain.user;

import com.graduate.novel.common.exception.BadRequestException;
import com.graduate.novel.common.mapper.UserMapper;
import com.graduate.novel.domain.role.Role;
import com.graduate.novel.domain.role.RoleRepository;
import com.graduate.novel.security.JwtService;
import com.graduate.novel.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final EmailService emailService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already exists");
        }

        // Determine which role to assign (default to USER)
        String roleName = (request.roleName() != null && !request.roleName().isBlank())
                ? request.roleName().toUpperCase()
                : Role.USER;

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BadRequestException("Role not found: " + roleName));

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .active(true)
                .role(role)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {} with role: {}", user.getEmail(), roleName);

        // Send welcome email
        emailService.sendWelcomeEmail(user.getEmail(), user.getDisplayName());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, userMapper.toDto(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (!user.isEnabled()) {
            throw new BadRequestException("User account is deactivated");
        }

        log.info("User logged in successfully: {}", user.getEmail());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, userMapper.toDto(user));
    }
}

