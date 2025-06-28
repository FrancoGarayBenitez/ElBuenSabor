package com.elbuensabor.controllers;

import com.elbuensabor.services.IAuth0Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller de fallback para endpoints /api/auth/*
 * Proporciona compatibilidad con el frontend que busca /api/auth/me
 */
@RestController
@RequestMapping("/api/auth")
public class AuthFallbackController {

    private static final Logger logger = LoggerFactory.getLogger(AuthFallbackController.class);
    private final IAuth0Service auth0Service;

    public AuthFallbackController(IAuth0Service auth0Service) {
        this.auth0Service = auth0Service;
    }

    /**
     * GET /api/auth/me
     * Endpoint de fallback para compatibilidad con el frontend
     * Redirige a la lógica de Auth0Service
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        try {
            logger.debug("🔍 Getting profile for Auth0 user (fallback endpoint): {}",
                    jwt != null ? jwt.getSubject() : "null");

            if (jwt == null) {
                logger.warn("❌ No JWT token provided for /api/auth/me");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "authenticated", false,
                                "error", "Token JWT requerido"
                        ));
            }

            Map<String, Object> profile = auth0Service.getCurrentUserProfile(jwt);

            logger.info("✅ Auth profile retrieved successfully for user: {}", jwt.getSubject());
            return ResponseEntity.ok(profile);

        } catch (Exception e) {
            logger.error("❌ Error getting current user profile (fallback): {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "authenticated", false,
                            "error", "Error interno del servidor",
                            "details", e.getMessage()
                    ));
        }
    }
}