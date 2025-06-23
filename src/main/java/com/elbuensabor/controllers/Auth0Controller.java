package com.elbuensabor.controllers;

import com.elbuensabor.dto.request.ClienteRegisterDTO;
import com.elbuensabor.dto.response.ClienteResponseDTO;
import com.elbuensabor.dto.response.LoginResponseDTO;
import com.elbuensabor.services.IAuth0Service;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth0")
public class Auth0Controller {

    private static final Logger logger = LoggerFactory.getLogger(Auth0Controller.class);

    private final IAuth0Service auth0Service;

    public Auth0Controller(IAuth0Service auth0Service) {
        this.auth0Service = auth0Service;
    }

    @PostMapping("/login")
    public ResponseEntity<?> handleAuth0Login(@AuthenticationPrincipal Jwt jwt,
                                              @RequestBody(required = false) Map<String, Object> userData) {
        try {
            if (jwt == null || !isUserToken(jwt)) {
                return ResponseEntity.badRequest().body(createErrorResponse("Token JWT de usuario de Auth0 requerido"));
            }

            logger.info("Processing Auth0 login for user: {}", jwt.getSubject());

            LoginResponseDTO response = auth0Service.processAuth0User(jwt, userData);

            return ResponseEntity.ok(createSuccessResponse(response, "Login exitoso con Auth0"));

        } catch (Exception e) {
            logger.error("Error in Auth0 login for user {}: {}", jwt.getSubject(), e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse("Error procesando login: " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerCliente(@AuthenticationPrincipal Jwt jwt,
                                             @Valid @RequestBody ClienteRegisterDTO registerDTO) {
        try {
            if (jwt == null || !isUserToken(jwt)) {
                return ResponseEntity.badRequest().body(createErrorResponse("Token JWT de usuario de Auth0 requerido"));
            }

            // Validar que el email coincida si está presente en el JWT
            String jwtEmail = jwt.getClaimAsString("email");
            if (jwtEmail != null && !jwtEmail.equals(registerDTO.getEmail())) {
                return ResponseEntity.badRequest().body(createErrorResponse("El email del token no coincide con el email de registro"));
            }

            logger.info("Processing Auth0 registration for user: {}", jwt.getSubject());

            ClienteResponseDTO clienteRegistrado = auth0Service.registerClienteFromAuth0(jwt, registerDTO);

            return ResponseEntity.ok(createSuccessResponse(clienteRegistrado, "Cliente registrado exitosamente"));

        } catch (Exception e) {
            logger.error("Error in Auth0 registration for user {}: {}", jwt.getSubject(), e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse("Error en registro: " + e.getMessage()));
        }
    }

    @PostMapping("/complete-profile")
    public ResponseEntity<?> completeProfile(@AuthenticationPrincipal Jwt jwt,
                                             @Valid @RequestBody ClienteRegisterDTO profileData) {
        try {
            if (jwt == null || !isUserToken(jwt)) {
                return ResponseEntity.badRequest().body(createErrorResponse("Token JWT de usuario de Auth0 requerido"));
            }

            // Validar que el email coincida si está presente en el JWT
            String jwtEmail = jwt.getClaimAsString("email");
            if (jwtEmail != null && !jwtEmail.equals(profileData.getEmail())) {
                return ResponseEntity.badRequest().body(createErrorResponse("El email del token no coincide con el email del perfil"));
            }

            logger.info("Completing profile for Auth0 user: {}", jwt.getSubject());

            ClienteResponseDTO updatedCliente = auth0Service.completeUserProfile(jwt, profileData);

            return ResponseEntity.ok(createSuccessResponse(updatedCliente, "Perfil completado exitosamente"));

        } catch (Exception e) {
            logger.error("Error completing profile for user {}: {}", jwt.getSubject(), e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse("Error completando perfil: " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        try {
            if (jwt == null) {
                return ResponseEntity.ok(Map.of("authenticated", false));
            }

            logger.debug("Getting profile for Auth0 user: {}", jwt.getSubject());

            Map<String, Object> profile = auth0Service.getCurrentUserProfile(jwt);

            return ResponseEntity.ok(profile);

        } catch (Exception e) {
            logger.error("Error getting current user: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("authenticated", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@AuthenticationPrincipal Jwt jwt) {
        try {
            if (jwt == null) {
                return ResponseEntity.ok(Map.of("valid", false));
            }

            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "sub", jwt.getSubject(),
                    "auth_provider", "auth0",
                    "token_type", getTokenType(jwt)
            ));

        } catch (Exception e) {
            logger.error("Error validating Auth0 token: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("valid", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/refresh-roles")
    public ResponseEntity<?> refreshUserRoles(@AuthenticationPrincipal Jwt jwt) {
        try {
            if (jwt == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("Token JWT requerido"));
            }

            logger.info("Refreshing roles for user: {}", jwt.getSubject());

            Map<String, Object> result = auth0Service.refreshUserRoles(jwt);

            return ResponseEntity.ok(createSuccessResponse(result, "Roles actualizados"));

        } catch (Exception e) {
            logger.error("Error refreshing roles for user {}: {}", jwt.getSubject(), e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse("Error refrescando roles: " + e.getMessage()));
        }
    }

    // === MÉTODOS PRIVADOS HELPER ===

    private boolean isUserToken(Jwt jwt) {
        String grantType = jwt.getClaimAsString("gty");
        return grantType == null || !"client-credentials".equals(grantType);
    }

    private String getTokenType(Jwt jwt) {
        String grantType = jwt.getClaimAsString("gty");
        return "client-credentials".equals(grantType) ? "machine-to-machine" : "user";
    }

    private Map<String, Object> createSuccessResponse(Object data, String message) {
        return Map.of(
                "success", true,
                "data", data,
                "message", message
        );
    }

    private Map<String, Object> createErrorResponse(String error) {
        return Map.of(
                "success", false,
                "error", error
        );
    }
}