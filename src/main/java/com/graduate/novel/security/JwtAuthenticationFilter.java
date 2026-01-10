package com.graduate.novel.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

       // Log request path for debugging
        log.debug("Processing request: {} {}", request.getMethod(), request.getRequestURI());

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token found in request to {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        log.debug("Bearer token found, extracting username...");

        try {
            username = jwtService.extractUsername(jwt);
            log.debug("Extracted username: {}", username);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                log.debug("Loaded user details for: {}", username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Successfully authenticated user: {}", username);
                } else {
                    log.warn("Token validation failed for user: {}", username);
                }
            } else if (username == null) {
                log.warn("Could not extract username from token");
            }
        } catch (Exception e) {
            // Log exception with details
            log.error("Cannot set user authentication for {}: {}", request.getRequestURI(), e.getMessage());
            log.debug("Full exception: ", e);
        }

        filterChain.doFilter(request, response);
    }
}

