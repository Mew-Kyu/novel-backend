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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

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
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Authentication endpoints - no token required
                        .requestMatchers("/api/auth/**").permitAll()
                        // Swagger/OpenAPI endpoints - no token required
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // Health check endpoints - no token required
                        .requestMatchers("/actuator/health", "/api/health", "/health", "/api/*/health", "/api/crawl/health").permitAll()
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
                        .requestMatchers(HttpMethod.GET, "/api/stories/*/detail").permitAll() // GET /api/stories/{id}/detail - NEW
                        .requestMatchers(HttpMethod.GET, "/api/stories/with-metadata").permitAll() // GET /api/stories/with-metadata - NEW
                        .requestMatchers(HttpMethod.GET, "/api/stories/featured").permitAll() // GET /api/stories/featured - NEW
                        .requestMatchers(HttpMethod.GET, "/api/stories/trending").permitAll() // GET /api/stories/trending - NEW
                        .requestMatchers(HttpMethod.POST, "/api/stories/*/view").permitAll() // POST /api/stories/{id}/view - NEW (no auth required)
                        // Public chapter endpoints - no token required
                        .requestMatchers(HttpMethod.GET, "/api/stories/*/chapters").permitAll() // GET /api/stories/{storyId}/chapters
                        .requestMatchers(HttpMethod.GET, "/api/stories/*/chapters/*").permitAll() // GET /api/stories/{storyId}/chapters/{chapterId}
                        .requestMatchers(HttpMethod.GET, "/api/chapters/latest").permitAll() // GET /api/chapters/latest
                        // Public stats endpoints - no token required
                        .requestMatchers(HttpMethod.GET, "/api/stats/summary").permitAll() // GET /api/stats/summary
                        // Public rating/comment read endpoints - no token required
                        .requestMatchers(HttpMethod.GET, "/api/ratings/story/*/average").permitAll() // GET average rating
                        .requestMatchers(HttpMethod.GET, "/api/ratings/story/*").permitAll() // GET ratings for a story
                        .requestMatchers(HttpMethod.GET, "/api/comments/story/*").permitAll() // GET comments for a story
                        .requestMatchers(HttpMethod.GET, "/api/comments/story/*/count").permitAll() // GET comment count
                        .requestMatchers(HttpMethod.GET, "/api/comments/*").permitAll() // GET single comment
                        // Export endpoints - requires authentication but allows all roles
                        .requestMatchers(HttpMethod.GET, "/api/export/*/epub").authenticated() // GET /api/export/{storyId}/epub
                        // Cloudinary upload endpoint - requires ADMIN or MODERATOR role
                        .requestMatchers(HttpMethod.POST, "/api/cloudinary/upload").hasAnyRole("ADMIN", "MODERATOR")
                        // Admin endpoints require ADMIN role (handled by @PreAuthorize, but adding explicit rule for clarity)
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
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
    @SuppressWarnings("deprecation")
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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow frontend origin
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://127.0.0.1:3000"
        ));
        // Allow all HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        // Allow all headers
        configuration.setAllowedHeaders(List.of("*"));
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        // Max age for preflight requests
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

