package com.example.Ecomm.config;

import com.example.Ecomm.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtUtil jwtService;
    private final UserService userService;

    // ✅ Constructor Injection (Sonar compliant)
    public JwtFilter(JwtUtil jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        logger.debug("JwtFilter: Processing request to {}", request.getRequestURI());

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                username = jwtService.extractUsername(token);
                logger.debug("JwtFilter: Token found, extracted username={}", username);
            } catch (Exception e) {
                logger.debug(
                        "JwtFilter: Error extracting username from token: {}",
                        e.getMessage()
                );
            }
        } else {
            logger.debug("JwtFilter: No Bearer token found in Authorization header");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = userService.loadUserByUsername(username);
            logger.debug(
                    "JwtFilter: UserDetails loaded for {}, roles={}",
                    username,
                    userDetails.getAuthorities()
            );

            // ✅ primitive boolean expression
            if (jwtService.validateToken(token, userDetails)) {

                logger.debug("JwtFilter: Token is VALID for user={}", username);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
                logger.debug("JwtFilter: SecurityContext updated for user={}", username);

            } else {
                logger.debug("JwtFilter: Token is INVALID for user={}", username);
            }

        } else if (username == null) {
            logger.debug("JwtFilter: Username is null or authentication already present");
        }

        filterChain.doFilter(request, response);
    }
}
