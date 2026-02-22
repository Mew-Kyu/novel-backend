package com.graduate.novel.domain.user;

import com.graduate.novel.common.exception.BadRequestException;
import com.graduate.novel.common.mapper.UserMapper;
import com.graduate.novel.domain.onboarding.UserOnboarding;
import com.graduate.novel.domain.onboarding.UserOnboardingRepository;
import com.graduate.novel.domain.recommendation.coldstart.ColdStartService;
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
    private final UserOnboardingRepository onboardingRepository;
    private final ColdStartService coldStartService;

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

        // Auto-create onboarding record so frontend knows onboarding is pending
        UserOnboarding onboarding = UserOnboarding.builder()
                .userId(user.getId())
                .completed(false)
                .build();
        onboardingRepository.save(onboarding);
        log.info("Created pending onboarding record for user {}", user.getId());

        // Send welcome email
        emailService.sendWelcomeEmail(user.getEmail(), user.getDisplayName());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // New users are always cold-start and need onboarding
        return new AuthResponse(accessToken, refreshToken, userMapper.toDto(user), true, true);
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

        boolean isColdStart = coldStartService.isUserColdStart(user.getId());
        boolean onboardingRequired = onboardingRepository.findByUserId(user.getId())
                .map(o -> !o.getCompleted())
                .orElse(true); // No record at all → treat as needing onboarding

        log.info("User {} cold-start={}, onboardingRequired={}", user.getId(), isColdStart, onboardingRequired);

        return new AuthResponse(accessToken, refreshToken, userMapper.toDto(user),
                isColdStart, onboardingRequired);
    }
}



