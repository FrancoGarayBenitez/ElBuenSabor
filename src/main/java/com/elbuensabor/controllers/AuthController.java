package com.elbuensabor.controllers;

import com.elbuensabor.dto.request.ClienteRegisterDTO;
import com.elbuensabor.dto.request.LoginRequestDTO;
import com.elbuensabor.dto.response.ClienteResponseDTO;
import com.elbuensabor.dto.response.LoginResponseDTO;
import com.elbuensabor.services.IAuthService;
<<<<<<< HEAD
import com.elbuensabor.services.impl.Auth0ServiceImpl;
=======
import com.elbuensabor.services.IAuth0Service;
>>>>>>> ramaLucho
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

<<<<<<< HEAD
    private final Auth0ServiceImpl auth0Service;
    private final IAuthService authService; // Servicio clásico

    @Autowired
    public AuthController(Auth0ServiceImpl auth0Service, IAuthService authService) {
        this.auth0Service = auth0Service;
=======
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final IAuthService authService;
    private final IAuth0Service auth0Service;

    @Autowired
    public AuthController(IAuthService authService, IAuth0Service auth0Service) {
>>>>>>> ramaLucho
        this.authService = authService;
        this.auth0Service = auth0Service;
    }

    // ========== SISTEMA CLÁSICO ========== //

    /**
     * Login clásico con email/password
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        LoginResponseDTO response = authService.authenticate(loginRequest);
        return ResponseEntity.ok(response);
    }

<<<<<<< HEAD
    /**
     * Validar token clásico
     */
    @PostMapping("/validate-classic")
    public ResponseEntity<Boolean> validateClassicToken(@RequestHeader("Authorization") String authHeader) {
=======
    @GetMapping("/validate")
    public ResponseEntity<Boolean> validateToken(@RequestHeader("Authorization") String authHeader,
                                                 @AuthenticationPrincipal Jwt jwt) {
        // Si hay un JWT de Auth0 autenticado, es válido
        if (jwt != null) {
            return ResponseEntity.ok(true);
        }

        // Si no, validar token local
>>>>>>> ramaLucho
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            boolean isValid = authService.validateToken(token);
            return ResponseEntity.ok(isValid);
        }

        return ResponseEntity.ok(false);
    }

<<<<<<< HEAD
    // ========== SISTEMA AUTH0 ==========

    /**
     * Registro de cliente - se llama después del registro en Auth0
     */
    @PostMapping("/register")
    public ResponseEntity<ClienteResponseDTO> registerCliente(
            @Valid @RequestBody ClienteRegisterDTO registerDTO,
            @AuthenticationPrincipal Jwt jwt) {

        String auth0Id = auth0Service.extractAuth0IdFromJwt(jwt);
        ClienteResponseDTO clienteRegistrado = auth0Service.registerClienteWithAuth0(registerDTO, auth0Id);
        return new ResponseEntity<>(clienteRegistrado, HttpStatus.CREATED);
    }

    /**
     * Verificar estado de autenticación Auth0
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        try {
            String email = auth0Service.extractEmailFromJwt(jwt);
            String auth0Id = auth0Service.extractAuth0IdFromJwt(jwt);

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("email", email);
            userInfo.put("auth0Id", auth0Id);
            userInfo.put("authenticated", true);

            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("authenticated", false);
            errorInfo.put("error", "Invalid token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorInfo);
        }
    }

    /**
     * Validar token Auth0
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();

        if (jwt != null) {
            response.put("valid", true);
            response.put("email", auth0Service.extractEmailFromJwt(jwt));
            response.put("auth0Id", auth0Service.extractAuth0IdFromJwt(jwt));
        } else {
            response.put("valid", false);
        }

        return ResponseEntity.ok(response);
=======
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
>>>>>>> ramaLucho
    }
}