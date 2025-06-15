package com.elbuensabor.controllers;

import com.elbuensabor.dto.request.LoginRequestDTO;
import com.elbuensabor.dto.response.LoginResponseDTO;
import com.elbuensabor.services.IAuthService;
import com.elbuensabor.services.IAuth0Service;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final IAuthService authService;
    private final IAuth0Service auth0Service;

    @Autowired
    public AuthController(IAuthService authService, IAuth0Service auth0Service) {
        this.authService = authService;
        this.auth0Service = auth0Service;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        LoginResponseDTO response = authService.authenticate(loginRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    public ResponseEntity<Boolean> validateToken(@RequestHeader("Authorization") String authHeader,
                                                 @AuthenticationPrincipal Jwt jwt) {
        // Si hay un JWT de Auth0 autenticado, es válido
        if (jwt != null) {
            return ResponseEntity.ok(true);
        }

        // Si no, validar token local
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            boolean isValid = authService.validateToken(token);
            return ResponseEntity.ok(isValid);
        }

        return ResponseEntity.ok(false);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                            @AuthenticationPrincipal Jwt jwt) {

        try {
            // Si es un usuario de Auth0
            if (jwt != null) {
                logger.info("Processing Auth0 user info request");

                // Extraer información disponible del JWT de Auth0
                String email = jwt.getClaimAsString("email");
                String name = jwt.getClaimAsString("name");
                String sub = jwt.getSubject();

                // Construir respuesta basada en la información disponible
                Map<String, Object> response = Map.of(
                        "sub", sub != null ? sub : "unknown",
                        "email", email != null ? email : "not_available",
                        "name", name != null ? name : "not_available",
                        "auth_provider", "auth0",
                        "token_type", getTokenType(jwt),
                        "valid", true
                );

                return ResponseEntity.ok(response);
            }

            // Si es un token local
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (authService.validateToken(token)) {
                    String email = authService.extractEmailFromToken(token);
                    return ResponseEntity.ok(Map.of(
                            "email", email != null ? email : "unknown",
                            "auth_provider", "local",
                            "valid", true
                    ));
                }
            }

            return ResponseEntity.ok(Map.of("valid", false));

        } catch (Exception e) {
            logger.error("Error in getCurrentUser: ", e);
            return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "error", e.getMessage()
            ));
        }
    }

    private String getTokenType(Jwt jwt) {
        try {
            String grantType = jwt.getClaimAsString("gty");
            if ("client-credentials".equals(grantType)) {
                return "machine-to-machine";
            }
            return "user";
        } catch (Exception e) {
            return "unknown";
        }
    }
}