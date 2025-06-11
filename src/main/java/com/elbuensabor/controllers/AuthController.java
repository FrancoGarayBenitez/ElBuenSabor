package com.elbuensabor.controllers;

import com.elbuensabor.dto.request.ClienteRegisterDTO;
import com.elbuensabor.dto.request.LoginRequestDTO;
import com.elbuensabor.dto.response.ClienteResponseDTO;
import com.elbuensabor.dto.response.LoginResponseDTO;
import com.elbuensabor.services.IAuthService;
import com.elbuensabor.services.impl.Auth0ServiceImpl;
import jakarta.validation.Valid;
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

    private final Auth0ServiceImpl auth0Service;
    private final IAuthService authService; // Servicio clásico

    @Autowired
    public AuthController(Auth0ServiceImpl auth0Service, IAuthService authService) {
        this.auth0Service = auth0Service;
        this.authService = authService;
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

    /**
     * Validar token clásico
     */
    @PostMapping("/validate-classic")
    public ResponseEntity<Boolean> validateClassicToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            boolean isValid = authService.validateToken(token);
            return ResponseEntity.ok(isValid);
        }
        return ResponseEntity.ok(false);
    }

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
    }
}