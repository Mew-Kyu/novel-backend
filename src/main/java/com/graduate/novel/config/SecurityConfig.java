package com.graduate.novel.config;

import com.graduate.novel.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Authentication endpoints - no token required
                        .requestMatchers("/api/auth/**").permitAll()
                        // Health check endpoints - no token required
                        .requestMatchers("/actuator/health", "/api/health", "/health", "/api/*/health").permitAll()
                        // AI semantic search endpoint - no token required
                        .requestMatchers(HttpMethod.POST, "/api/ai/search/semantic").permitAll()
                        // Public genre READ endpoints - no token required (GET only)
                        .requestMatchers(HttpMethod.GET, "/api/genres", "/api/genres/**").permitAll()
                        // Genre CUD endpoints require authentication (POST, PUT, DELETE)
                        .requestMatchers(HttpMethod.POST, "/api/genres").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/genres/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/genres/**").authenticated()
                        // Public story endpoints - no token required
                        .requestMatchers(HttpMethod.GET, "/api/stories").permitAll() // GET /api/stories
                        .requestMatchers(HttpMethod.GET, "/api/stories/*").permitAll() // GET /api/stories/{id}
                        // Public chapter endpoints - no token required
                        .requestMatchers(HttpMethod.GET, "/api/stories/*/chapters").permitAll() // GET /api/stories/{storyId}/chapters
                        .requestMatchers(HttpMethod.GET, "/api/stories/*/chapters/*").permitAll() // GET /api/stories/{storyId}/chapters/{chapterId}
                        // Public rating/comment read endpoints - no token required
                        .requestMatchers(HttpMethod.GET, "/api/ratings/story/*/average").permitAll() // GET average rating
                        .requestMatchers(HttpMethod.GET, "/api/ratings/story/*").permitAll() // GET ratings for a story
                        .requestMatchers(HttpMethod.GET, "/api/comments/story/*").permitAll() // GET comments for a story
                        .requestMatchers(HttpMethod.GET, "/api/comments/story/*/count").permitAll() // GET comment count
                        .requestMatchers(HttpMethod.GET, "/api/comments/*").permitAll() // GET single comment
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

