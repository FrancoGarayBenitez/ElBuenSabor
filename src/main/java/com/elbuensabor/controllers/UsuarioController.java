package com.elbuensabor.controllers;

import com.elbuensabor.dto.response.UsuarioGridResponseDTO;
import com.elbuensabor.services.IUsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador para gestión de usuarios por parte del administrador
 * Incluye operaciones de cambio de rol, activación/desactivación, etc.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioController.class);
    private final IUsuarioService usuarioService;

    // ==================== ENDPOINTS DE LISTADO ====================

    /**
     * GET /api/usuarios/grilla
     * Obtiene todos los usuarios para mostrar en la grilla administrativa
     */
    @GetMapping("/grilla")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioGridResponseDTO>> obtenerUsuariosParaGrilla() {
        logger.info("🎯 Admin solicitando grilla de usuarios");
        List<UsuarioGridResponseDTO> usuarios = usuarioService.obtenerUsuariosParaGrilla();
        logger.debug("✅ Retornando {} usuarios", usuarios.size());
        return ResponseEntity.ok(usuarios);
    }

    /**
     * GET /api/usuarios/{id}
     * Obtiene detalles específicos de un usuario
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioGridResponseDTO> obtenerUsuarioPorId(@PathVariable Long id) {
        logger.debug("Admin solicitando detalles del usuario ID: {}", id);
        UsuarioGridResponseDTO usuario = usuarioService.obtenerUsuarioPorId(id);
        return ResponseEntity.ok(usuario);
    }

    // ==================== ENDPOINTS DE GESTIÓN DE ROLES ====================

    /**
     * PUT /api/usuarios/rol
     * Cambia el rol de un usuario
     */
    @PutMapping("/rol")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> cambiarRolUsuario(
            @Valid @RequestBody CambiarRolRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        logger.info("🔄 Admin {} cambiando rol de usuario {} a {}",
                jwt.getSubject(), request.getIdUsuario(), request.getNuevoRol());

        try {
            // Validaciones de seguridad
            validarCambioRol(request, jwt);

            // Ejecutar cambio
            UsuarioGridResponseDTO usuarioActualizado = usuarioService.cambiarRol(
                    request.getIdUsuario(),
                    request.getNuevoRol()
            );

            logger.info("✅ Rol cambiado exitosamente para usuario {}: {} -> {}",
                    request.getIdUsuario(),
                    request.getRolAnterior() != null ? request.getRolAnterior() : "N/A",
                    request.getNuevoRol());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Rol actualizado correctamente",
                    "data", usuarioActualizado,
                    "cambio", Map.of(
                            "rolAnterior", request.getRolAnterior(),
                            "rolNuevo", request.getNuevoRol(),
                            "usuario", usuarioActualizado.getEmail()
                    )
            ));

        } catch (SecurityException e) {
            logger.warn("❌ Violación de seguridad en cambio de rol: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "code", "SECURITY_VIOLATION"
            ));

        } catch (Exception e) {
            logger.error("❌ Error cambiando rol de usuario {}: {}", request.getIdUsuario(), e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Error interno al cambiar rol: " + e.getMessage(),
                    "code", "INTERNAL_ERROR"
            ));
        }
    }

    // ==================== ENDPOINTS DE GESTIÓN DE ESTADO ====================

    /**
     * PUT /api/usuarios/estado
     * Activa o desactiva un usuario
     */
    @PutMapping("/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> cambiarEstadoUsuario(
            @Valid @RequestBody CambiarEstadoRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        logger.info("🔄 Admin {} {} usuario {}",
                jwt.getSubject(),
                request.isActivo() ? "activando" : "desactivando",
                request.getIdUsuario());

        try {
            // Validaciones de seguridad
            validarCambioEstado(request, jwt);

            // Ejecutar cambio
            UsuarioGridResponseDTO usuarioActualizado = usuarioService.cambiarEstado(
                    request.getIdUsuario(),
                    request.isActivo()
            );

            logger.info("✅ Estado cambiado exitosamente para usuario {}: {}",
                    request.getIdUsuario(), request.isActivo() ? "ACTIVO" : "INACTIVO");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Estado actualizado correctamente",
                    "data", usuarioActualizado,
                    "estado", request.isActivo() ? "ACTIVO" : "INACTIVO"
            ));

        } catch (SecurityException e) {
            logger.warn("❌ Violación de seguridad en cambio de estado: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "code", "SECURITY_VIOLATION"
            ));

        } catch (Exception e) {
            logger.error("❌ Error cambiando estado de usuario {}: {}", request.getIdUsuario(), e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Error interno al cambiar estado: " + e.getMessage(),
                    "code", "INTERNAL_ERROR"
            ));
        }
    }

    // ==================== ENDPOINTS DE DEBUG ====================

    /**
     * GET /api/usuarios/debug
     * Endpoint temporal para diagnosticar problemas de autenticación
     * ⚠️ ELIMINAR EN PRODUCCIÓN
     */
    @GetMapping("/debug")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> debugAuth(@AuthenticationPrincipal Jwt jwt, Authentication auth) {
        logger.info("🔍 DEBUG: Verificando autenticación de admin");

        return ResponseEntity.ok(Map.of(
                "jwt_present", jwt != null,
                "jwt_subject", jwt != null ? jwt.getSubject() : "null",
                "auth_present", auth != null,
                "auth_name", auth != null ? auth.getName() : "null",
                "authorities", auth != null ? auth.getAuthorities().stream()
                        .map(a -> a.getAuthority()).collect(Collectors.toList()) : "null",
                "jwt_claims", jwt != null ? jwt.getClaims().keySet() : "null",
                "message", "Debug de autenticación para admin"
        ));
    }

    // ==================== MÉTODOS DE VALIDACIÓN ====================

    /**
     * Valida si el cambio de rol es seguro y permitido
     */
    private void validarCambioRol(CambiarRolRequest request, Jwt jwt) {
        // Obtener ID del usuario actual del JWT
        Long currentUserId = usuarioService.obtenerIdUsuarioPorAuth0Id(jwt.getSubject());

        // No permitir que el admin se quite su propio rol ADMIN
        if (currentUserId.equals(request.getIdUsuario()) &&
                "ADMIN".equals(request.getRolAnterior()) &&
                !"ADMIN".equals(request.getNuevoRol())) {
            throw new SecurityException("No puedes cambiar tu propio rol de administrador");
        }

        // Validar que el nuevo rol sea válido
        if (!esRolValido(request.getNuevoRol())) {
            throw new IllegalArgumentException("Rol inválido: " + request.getNuevoRol());
        }

        // Advertencia especial para asignación de ADMIN
        if ("ADMIN".equals(request.getNuevoRol()) && !"ADMIN".equals(request.getRolAnterior())) {
            logger.warn("⚠️ Admin {} está otorgando permisos de ADMIN a usuario {}",
                    jwt.getSubject(), request.getIdUsuario());
        }
    }

    /**
     * Valida si el cambio de estado es seguro y permitido
     */
    private void validarCambioEstado(CambiarEstadoRequest request, Jwt jwt) {
        // Obtener ID del usuario actual del JWT
        Long currentUserId = usuarioService.obtenerIdUsuarioPorAuth0Id(jwt.getSubject());

        // No permitir que el admin se desactive a sí mismo
        if (currentUserId.equals(request.getIdUsuario()) && !request.isActivo()) {
            throw new SecurityException("No puedes desactivar tu propia cuenta");
        }

        // Verificar que no sea el último admin (si se va a desactivar un admin)
        if (!request.isActivo()) {
            UsuarioGridResponseDTO usuario = usuarioService.obtenerUsuarioPorId(request.getIdUsuario());
            if ("ADMIN".equals(usuario.getRol())) {
                long adminCount = usuarioService.contarAdministradoresActivos();
                if (adminCount <= 1) {
                    throw new SecurityException("No se puede desactivar el último administrador del sistema");
                }
            }
        }
    }

    /**
     * Verifica si un rol es válido
     */
    private boolean esRolValido(String rol) {
        return List.of("CLIENTE", "CAJERO", "COCINERO", "DELIVERY", "ADMIN").contains(rol);
    }

    // ==================== CLASES DE REQUEST ====================

    /**
     * DTO para cambio de rol
     */
    public static class CambiarRolRequest {
        private Long idUsuario;
        private String nuevoRol;
        private String rolAnterior; // Para logging y validación

        // Constructors, getters y setters
        public CambiarRolRequest() {}

        public CambiarRolRequest(Long idUsuario, String nuevoRol, String rolAnterior) {
            this.idUsuario = idUsuario;
            this.nuevoRol = nuevoRol;
            this.rolAnterior = rolAnterior;
        }

        public Long getIdUsuario() { return idUsuario; }
        public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }

        public String getNuevoRol() { return nuevoRol; }
        public void setNuevoRol(String nuevoRol) { this.nuevoRol = nuevoRol; }

        public String getRolAnterior() { return rolAnterior; }
        public void setRolAnterior(String rolAnterior) { this.rolAnterior = rolAnterior; }
    }

    /**
     * DTO para cambio de estado
     */
    public static class CambiarEstadoRequest {
        private Long idUsuario;
        private boolean activo;

        // Constructors, getters y setters
        public CambiarEstadoRequest() {}

        public CambiarEstadoRequest(Long idUsuario, boolean activo) {
            this.idUsuario = idUsuario;
            this.activo = activo;
        }

        public Long getIdUsuario() { return idUsuario; }
        public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }

        public boolean isActivo() { return activo; }
        public void setActivo(boolean activo) { this.activo = activo; }
    }
}