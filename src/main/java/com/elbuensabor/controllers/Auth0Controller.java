package com.elbuensabor.controllers;

import com.elbuensabor.dto.response.LoginResponseDTO;
import com.elbuensabor.services.IAuth0Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth0")

public class Auth0Controller {

    private static final Logger logger = LoggerFactory.getLogger(Auth0Controller.class);
    private final IAuth0Service auth0Service;

    @Autowired
    public Auth0Controller(IAuth0Service auth0Service) {
        this.auth0Service = auth0Service;
    }

    @PostMapping("/login")
    public ResponseEntity<?> handleAuth0Login(@AuthenticationPrincipal Jwt jwt,
                                              @RequestBody(required = false) Map<String, Object> userData) {
        try {
            if (jwt == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Token JWT requerido"
                ));
            }

            logger.info("=== DATOS RECIBIDOS DEL FRONTEND ===");
            if (userData != null) {
                logger.info("User data from frontend: {}", userData);
            } else {
                logger.info("No user data received from frontend");
            }

            // Solo procesar si es un token de usuario (no machine-to-machine)
            if (isUserToken(jwt)) {
                // Pasar los datos del frontend al servicio
                LoginResponseDTO response = auth0Service.processAuth0User(jwt, userData);

                Map<String, Object> responseBody = new HashMap<>();
                responseBody.put("success", true);
                responseBody.put("user", response);
                responseBody.put("message", "Login con Auth0 exitoso");

                return ResponseEntity.ok(responseBody);
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Este endpoint requiere un token de usuario, no de aplicación"
                ));
            }

        } catch (Exception e) {
            logger.error("Error in Auth0 login: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Error procesando usuario de Auth0: " + e.getMessage()
            ));
        }
    }
    @GetMapping("/profile")
    public ResponseEntity<?> getAuth0Profile(@AuthenticationPrincipal Jwt jwt) {
        try {
            if (jwt == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Token JWT requerido"
                ));
            }

            Map<String, Object> profile = new HashMap<>();
            profile.put("sub", jwt.getSubject());
            profile.put("iss", jwt.getIssuer() != null ? jwt.getIssuer().toString() : null);
            profile.put("token_type", getTokenType(jwt));

            // Solo agregar claims de usuario si existen
            if (isUserToken(jwt)) {
                profile.put("email", jwt.getClaimAsString("email"));
                profile.put("name", jwt.getClaimAsString("name"));
                profile.put("given_name", jwt.getClaimAsString("given_name"));
                profile.put("family_name", jwt.getClaimAsString("family_name"));
                profile.put("picture", jwt.getClaimAsString("picture"));
            } else {
                profile.put("client_id", jwt.getClaimAsString("azp"));
                profile.put("permissions", jwt.getClaim("permissions"));
            }

            profile.put("roles", jwt.getClaim("https://APIElBuenSabor/roles"));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "profile", profile
            ));

        } catch (Exception e) {
            logger.error("Error getting Auth0 profile: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateAuth0Token(@AuthenticationPrincipal Jwt jwt) {
        try {
            if (jwt == null) {
                return ResponseEntity.ok(Map.of("valid", false));
            }

            // Si llegamos aquí, el token es válido (Spring Security ya lo validó)
            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("sub", jwt.getSubject());
            response.put("token_type", getTokenType(jwt));
            response.put("auth_provider", "auth0");

            // Solo agregar email si existe
            String email = jwt.getClaimAsString("email");
            if (email != null) {
                response.put("email", email);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error validating Auth0 token: ", e);
            return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "error", e.getMessage()
            ));
        }
    }

    private boolean isUserToken(Jwt jwt) {
        // Los tokens de usuario tienen 'gty' diferente o no tienen 'gty'
        // Los tokens machine-to-machine tienen 'gty': 'client-credentials'
        String grantType = jwt.getClaimAsString("gty");
        return grantType == null || !"client-credentials".equals(grantType);
    }

    private String getTokenType(Jwt jwt) {
        String grantType = jwt.getClaimAsString("gty");
        if ("client-credentials".equals(grantType)) {
            return "machine-to-machine";
        }
        return "user";
    }
}