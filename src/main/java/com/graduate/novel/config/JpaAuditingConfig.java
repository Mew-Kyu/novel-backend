package com.graduate.novel.config;

import com.graduate.novel.domain.user.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Configuration for JPA Auditing.
 * Enables automatic tracking of entity creation and modification metadata.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    /**
     * Provides the current auditor (user ID) for JPA auditing.
     * Returns the ID of the currently authenticated user, or empty if anonymous/system.
     *
     * @return AuditorAware bean that retrieves current user ID
     */
    @Bean
    public AuditorAware<Long> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // Return empty if no authentication or anonymous (e.g., system background jobs)
            if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
                return Optional.empty();
            }

            // Get the principal (should be User from UserDetailsService)
            Object principal = authentication.getPrincipal();

            if (principal instanceof User user) {
                return Optional.ofNullable(user.getId());
            }

            // Fallback: return empty if principal is not a User instance
            return Optional.empty();
        };
    }
}

