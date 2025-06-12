package com.elbuensabor.controllers;

import com.elbuensabor.entities.Rol;
import com.elbuensabor.services.IRoleManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> changeUserRole(
            @PathVariable Long clienteId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal Jwt jwt) {

        try {
            // Verificar que quien hace la petición es ADMIN
            if (!roleManagementService.isAdmin(jwt.getSubject())) {
                return ResponseEntity.status(403).body("Solo los administradores pueden cambiar roles");
            }

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
    public ResponseEntity<?> getAllUsersWithRoles(@AuthenticationPrincipal Jwt jwt) {
        try {
            if (!roleManagementService.isAdmin(jwt.getSubject())) {
                return ResponseEntity.status(403).body("Solo los administradores pueden ver esta información");
            }

            return ResponseEntity.ok(roleManagementService.getAllUsersWithRoles());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}