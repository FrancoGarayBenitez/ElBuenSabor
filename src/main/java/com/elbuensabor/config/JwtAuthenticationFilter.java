package com.elbuensabor.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String token = null;
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);
        }

        // Solo procesar si no hay autenticación previa y el token es local
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (isLocalToken(token)) {
                try {
                    handleLocalToken(token, request);
                } catch (Exception e) {
                    logger.warn("Error processing local JWT token: " + e.getMessage());
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLocalToken(String token) {
        try {
            String username = jwtUtil.extractUsername(token);
            boolean isExpired = jwtUtil.isTokenExpired(token);
            return username != null && !isExpired;
        } catch (Exception e) {
            return false;
        }
    }

    private void handleLocalToken(String token, HttpServletRequest request) {
        String username = jwtUtil.extractUsername(token);
        String role = jwtUtil.extractRole(token);

        if (role == null) {
            role = "ROLE_CLIENTE";
        }

        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role.toUpperCase();
        }

        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(role)
        );

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                username, null, authorities
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}