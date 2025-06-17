package com.elbuensabor.controllers;

import com.elbuensabor.dto.response.ClienteResponseDTO;
import com.elbuensabor.services.IAuth0SyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IAuth0SyncService auth0SyncService;

    @Autowired
    public AuthController(IAuth0SyncService auth0SyncService) {
        this.auth0SyncService = auth0SyncService;
    }

    /**
     * Endpoint para manejar el callback de Auth0 y sincronizar el usuario
     * Este endpoint se llamará desde el frontend después del login exitoso
     */
    @PostMapping("/callback")
    public ResponseEntity<ClienteResponseDTO> handleAuth0Callback(@AuthenticationPrincipal Jwt jwt) {
        try {
            // Sincronizar usuario de Auth0 con nuestra base de datos
            ClienteResponseDTO cliente = auth0SyncService.syncUserFromAuth0(jwt);
            return ResponseEntity.ok(cliente);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Endpoint alternativo que recibe los datos del usuario directamente
     */
    @PostMapping("/callback-direct")
    public ResponseEntity<ClienteResponseDTO> handleAuth0CallbackDirect(
            @RequestBody Map<String, Object> userData,
            @AuthenticationPrincipal Jwt jwt) { // <-- 1. Añade @AuthenticationPrincipal Jwt jwt
        try {
            System.out.println("Recibiendo datos directos del usuario: " + userData);

            // 2. Pasa el token JWT al servicio
            ClienteResponseDTO cliente = auth0SyncService.syncUserFromUserData(userData, jwt);
            return ResponseEntity.ok(cliente);
        } catch (Exception e) {
            System.out.println("Error en callback-direct: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Obtener información del usuario autenticado
     */
    @GetMapping("/me")
    public ResponseEntity<ClienteResponseDTO> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        try {
            ClienteResponseDTO cliente = auth0SyncService.getCurrentUserInfo(jwt);
            return ResponseEntity.ok(cliente);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Validar token de Auth0 y mostrar info del usuario
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@AuthenticationPrincipal Jwt jwt) {
        // Obtener información del usuario desde nuestra BD
        ClienteResponseDTO cliente = auth0SyncService.getCurrentUserInfo(jwt);

        Map<String, Object> response = new HashMap<>();
        response.put("valid", true);
        response.put("sub", jwt.getSubject());
        response.put("email", jwt.getClaimAsString("email"));
        response.put("role", cliente.getRol()); // Obtener rol desde nuestra BD
        response.put("cliente", cliente);

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para obtener la configuración de Auth0 para el frontend
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getAuth0Config() {
        Map<String, String> config = new HashMap<>();
        config.put("domain", "dev-ik2kub20ymu4sfpr.us.auth0.com");
        config.put("clientId", "4u4F4fKQrsD9Bvvh9ODZ0tnqzR431TBV");
        config.put("audience", "http://localhost:8080/api");
        config.put("redirectUri", "http://localhost:5173/callback"); // Ajusta según tu frontend
        return ResponseEntity.ok(config);
    }
}