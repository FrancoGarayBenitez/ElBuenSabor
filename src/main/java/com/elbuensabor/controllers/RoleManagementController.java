package com.elbuensabor.controllers;

import com.elbuensabor.entities.Rol;
import com.elbuensabor.services.IRoleManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // <-- Importante
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/roles")
public class RoleManagementController {

    private final IRoleManagementService roleManagementService;

    @Autowired
    public RoleManagementController(IRoleManagementService roleManagementService) {
        this.roleManagementService = roleManagementService;
    }

    /**
     * Cambiar rol de un usuario (solo ADMIN puede hacer esto)
     */
    @PutMapping("/change-role/{clienteId}")
    @PreAuthorize("hasRole('ADMIN')") // <-- 1. AHORA LA SEGURIDAD ES DECLARATIVA
    public ResponseEntity<?> changeUserRole(
            @PathVariable Long clienteId,
            @RequestBody Map<String, String> request) { // <-- 2. Ya no necesitamos el JWT aquí

        try {
            // Ya no es necesaria la verificación manual con if
            String newRole = request.get("role");
            Rol rol = Rol.valueOf(newRole.toUpperCase());

            roleManagementService.changeUserRole(clienteId, rol);

            return ResponseEntity.ok("Rol actualizado correctamente");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al cambiar rol: " + e.getMessage());
        }
    }

    /**
     * Listar todos los usuarios con sus roles (solo ADMIN)
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')") // <-- 3. PROTEGIDO CON LA ANOTACIÓN
    public ResponseEntity<?> getAllUsersWithRoles() { // <-- 4. Ya no necesitamos el JWT aquí
        try {
            // La anotación @PreAuthorize ya hizo el trabajo de seguridad
            return ResponseEntity.ok(roleManagementService.getAllUsersWithRoles());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}