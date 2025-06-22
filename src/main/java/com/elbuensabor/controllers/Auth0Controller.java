package com.elbuensabor.controllers;

import com.elbuensabor.dto.request.ClienteRegisterDTO;
import com.elbuensabor.dto.response.ClienteResponseDTO;
import com.elbuensabor.dto.response.LoginResponseDTO;
import com.elbuensabor.entities.Cliente;
import com.elbuensabor.entities.Domicilio;
import com.elbuensabor.entities.Imagen;
import com.elbuensabor.entities.Rol;
import com.elbuensabor.repository.IClienteRepository;
import com.elbuensabor.services.IAuth0Service;
import com.elbuensabor.services.IClienteService;
import com.elbuensabor.services.mapper.ClienteMapper;
import com.elbuensabor.services.mapper.DomicilioMapper;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador para manejo de autenticación y registro con Auth0
 * Reemplaza completamente la autenticación local
 */
@RestController
@RequestMapping("/api/auth0")
public class Auth0Controller {

    private static final Logger logger = LoggerFactory.getLogger(Auth0Controller.class);

    @Autowired
    private IAuth0Service auth0Service;

    @Autowired
    private IClienteService clienteService;

    @Autowired
    private IClienteRepository clienteRepository;

    @Autowired
    private ClienteMapper clienteMapper;

    @Autowired
    private DomicilioMapper domicilioMapper;


    /**
     * Procesa el login de usuarios autenticados con Auth0
     * Sincroniza o crea el usuario en la base de datos local
     */
    @PostMapping("/login")
    public ResponseEntity<?> handleAuth0Login(@AuthenticationPrincipal Jwt jwt,
                                              @RequestBody(required = false) Map<String, Object> userData) {
        try {
            if (jwt == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Token JWT de Auth0 requerido"
                ));
            }

            // Validar que sea un token de usuario (no machine-to-machine)
            if (!isUserToken(jwt)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Se requiere un token de usuario de Auth0"
                ));
            }

            logger.info("Processing Auth0 login for user: {}", jwt.getSubject());

            try {
                // Procesar usuario y sincronizar con BD local
                LoginResponseDTO response = auth0Service.processAuth0User(jwt, userData);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "user", response,
                        "message", "Login exitoso con Auth0"
                ));

            } catch (Exception serviceException) {
                // Si el usuario ya existe, intentar obtenerlo directamente
                if (serviceException.getMessage() != null &&
                        (serviceException.getMessage().contains("Duplicate entry") ||
                                serviceException.getMessage().contains("already exists"))) {

                    logger.warn("User already exists, attempting to retrieve existing user: {}", jwt.getSubject());

                    try {
                        // Buscar usuario existente por Auth0 ID
                        Cliente existingCliente = auth0Service.findOrCreateClienteFromAuth0(
                                jwt.getSubject(),
                                jwt.getClaimAsString("email"),
                                jwt.getClaimAsString("given_name") != null ? jwt.getClaimAsString("given_name") : "Usuario",
                                jwt.getClaimAsString("family_name") != null ? jwt.getClaimAsString("family_name") : "Auth0",
                                extractRoleFromJwt(jwt)
                        );

                        // Construir respuesta con usuario existente
                        LoginResponseDTO response = new LoginResponseDTO(
                                jwt.getTokenValue(),
                                existingCliente.getUsuario().getEmail(),
                                existingCliente.getUsuario().getRol().name(),
                                existingCliente.getUsuario().getIdUsuario(),
                                existingCliente.getNombre(),
                                existingCliente.getApellido()
                        );

                        return ResponseEntity.ok(Map.of(
                                "success", true,
                                "user", response,
                                "message", "Login exitoso - usuario existente"
                        ));

                    } catch (Exception fallbackException) {
                        logger.error("Error retrieving existing user: ", fallbackException);
                        return ResponseEntity.badRequest().body(Map.of(
                                "success", false,
                                "error", "Error procesando usuario existente: " + fallbackException.getMessage()
                        ));
                    }
                } else {
                    // Error diferente, propagarlo
                    throw serviceException;
                }
            }

        } catch (Exception e) {
            logger.error("Error in Auth0 login for user {}: {}",
                    jwt != null ? jwt.getSubject() : "unknown", e.getMessage());

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Error procesando login: " + e.getMessage()
            ));
        }
    }

    /**
     * Extrae el rol del JWT de Auth0
     */
    private com.elbuensabor.entities.Rol extractRoleFromJwt(Jwt jwt) {
        Object rolesObj = jwt.getClaim("https://APIElBuenSabor/roles");

        if (rolesObj instanceof java.util.List) {
            java.util.List<?> roles = (java.util.List<?>) rolesObj;
            if (!roles.isEmpty()) {
                String role = roles.get(0).toString().toUpperCase();
                try {
                    return com.elbuensabor.entities.Rol.valueOf(role);
                } catch (IllegalArgumentException e) {
                    return com.elbuensabor.entities.Rol.CLIENTE;
                }
            }
        }

        return com.elbuensabor.entities.Rol.CLIENTE;
    }

    /**
     * Registra un nuevo cliente con datos adicionales
     * El usuario ya debe estar registrado en Auth0
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerCliente(@AuthenticationPrincipal Jwt jwt,
                                             @Valid @RequestBody ClienteRegisterDTO registerDTO) {
        try {
            if (jwt == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Token JWT de Auth0 requerido para registro"
                ));
            }

            if (!isUserToken(jwt)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Se requiere un token de usuario de Auth0"
                ));
            }

            logger.info("Processing Auth0 registration for user: {}", jwt.getSubject());

            // Verificar que el email del JWT coincida con el del registro
            String jwtEmail = jwt.getClaimAsString("email");
            if (jwtEmail != null && !jwtEmail.equals(registerDTO.getEmail())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "El email del token no coincide con el email de registro"
                ));
            }

            // Registrar cliente con datos adicionales (sin password)
            ClienteResponseDTO clienteRegistrado = auth0Service.registerClienteFromAuth0(jwt, registerDTO);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "cliente", clienteRegistrado,
                    "message", "Cliente registrado exitosamente"
            ));

        } catch (Exception e) {
            logger.error("Error in Auth0 registration for user {}: {}",
                    jwt != null ? jwt.getSubject() : "unknown", e.getMessage());

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Error en registro: " + e.getMessage()
            ));
        }
    }

    /**
     * Obtiene el perfil del usuario autenticado
     * Reemplaza el endpoint /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        try {
            if (jwt == null) {
                return ResponseEntity.ok(Map.of("authenticated", false));
            }

            logger.debug("Getting profile for Auth0 user: {}", jwt.getSubject());

            // Construir información del perfil
            Map<String, Object> profile = new HashMap<>();
            profile.put("authenticated", true);
            profile.put("auth_provider", "auth0");
            profile.put("sub", jwt.getSubject());
            profile.put("token_type", getTokenType(jwt));

            // Agregar claims disponibles del usuario
            String email = jwt.getClaimAsString("email");
            if (email != null) {
                profile.put("email", email);
            }

            String name = jwt.getClaimAsString("name");
            if (name != null) {
                profile.put("name", name);
            }

            // Agregar roles
            Object roles = jwt.getClaim("https://APIElBuenSabor/roles");
            if (roles != null) {
                profile.put("roles", roles);
            }

            return ResponseEntity.ok(profile);

        } catch (Exception e) {
            logger.error("Error getting current user: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "authenticated", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Valida si el token de Auth0 es válido
     * Reemplaza el endpoint /api/auth/validate
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@AuthenticationPrincipal Jwt jwt) {
        try {
            if (jwt == null) {
                return ResponseEntity.ok(Map.of("valid", false));
            }

            // Si llegamos aquí, Spring Security ya validó el token
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "sub", jwt.getSubject(),
                    "auth_provider", "auth0",
                    "token_type", getTokenType(jwt)
            ));

        } catch (Exception e) {
            logger.error("Error validating Auth0 token: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Verifica si el token es de un usuario (no machine-to-machine)
     */
    private boolean isUserToken(Jwt jwt) {
        String grantType = jwt.getClaimAsString("gty");
        return grantType == null || !"client-credentials".equals(grantType);
    }

    /**
     * Determina el tipo de token Auth0
     */
    private String getTokenType(Jwt jwt) {
        String grantType = jwt.getClaimAsString("gty");
        return "client-credentials".equals(grantType) ? "machine-to-machine" : "user";
    }

    /**
     * Completa el perfil de un usuario ya autenticado con Auth0
     * Actualiza datos faltantes sin intentar crear un nuevo usuario
     */
    @PostMapping("/complete-profile")
    public ResponseEntity<?> completeProfile(@AuthenticationPrincipal Jwt jwt,
                                             @Valid @RequestBody ClienteRegisterDTO profileData) {
        try {
            if (jwt == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Token JWT de Auth0 requerido"
                ));
            }

            if (!isUserToken(jwt)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Se requiere un token de usuario de Auth0"
                ));
            }

            logger.info("Completing profile for Auth0 user: {}", jwt.getSubject());

            // Verificar que el email del JWT coincida con el del perfil
            String jwtEmail = jwt.getClaimAsString("email");
            if (jwtEmail != null && !jwtEmail.equals(profileData.getEmail())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "El email del token no coincide con el email del perfil"
                ));
            }

            // Buscar cliente existente
            Optional<Cliente> existingCliente = clienteRepository.findByUsuarioAuth0Id(jwt.getSubject());

            if (existingCliente.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Usuario no encontrado. Debe hacer login primero."
                ));
            }

            Cliente cliente = existingCliente.get();

            // Actualizar datos del cliente
            cliente.setNombre(profileData.getNombre());
            cliente.setApellido(profileData.getApellido());
            cliente.setTelefono(profileData.getTelefono());
            cliente.setFechaNacimiento(profileData.getFechaNacimiento());

            // Actualizar email del usuario si es diferente
            if (profileData.getEmail() != null && !profileData.getEmail().isEmpty()) {
                cliente.getUsuario().setEmail(profileData.getEmail());
            }

            // Actualizar domicilio (limpiar existentes y agregar nuevo)
            if (profileData.getDomicilio() != null) {
                // Limpiar domicilios existentes
                cliente.getDomicilios().clear();

                // Crear nuevo domicilio
                Domicilio domicilio = domicilioMapper.toEntity(profileData.getDomicilio());
                domicilio.setCliente(cliente);
                cliente.getDomicilios().add(domicilio);
            }

            // Actualizar imagen si está presente
            if (profileData.getImagen() != null) {
                Imagen imagen = new Imagen();
                imagen.setDenominacion(profileData.getImagen().getDenominacion());
                imagen.setUrl(profileData.getImagen().getUrl());
                cliente.setImagen(imagen);
            }

            // Guardar cambios
            Cliente updatedCliente = clienteRepository.save(cliente);
            ClienteResponseDTO clienteResponse = clienteMapper.toDTO(updatedCliente);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "cliente", clienteResponse,
                    "message", "Perfil completado exitosamente"
            ));

        } catch (Exception e) {
            logger.error("Error completing profile for user {}: {}",
                    jwt != null ? jwt.getSubject() : "unknown", e.getMessage());

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Error completando perfil: " + e.getMessage()
            ));
        }
    }

    /**
     * Fuerza la actualización de roles desde Auth0
     * Útil cuando se cambian roles en Auth0 Dashboard
     */
    @PostMapping("/refresh-roles")
    public ResponseEntity<?> refreshUserRoles(@AuthenticationPrincipal Jwt jwt) {
        try {
            if (jwt == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Token JWT requerido"
                ));
            }

            logger.info("Refreshing roles for user: {}", jwt.getSubject());

            // Extraer roles actualizados del nuevo token
            Rol updatedRole = extractRoleFromJwt(jwt);

            // Buscar cliente y actualizar rol en BD local
            Optional<Cliente> clienteOpt = clienteRepository.findByUsuarioAuth0Id(jwt.getSubject());
            if (clienteOpt.isPresent()) {
                Cliente cliente = clienteOpt.get();
                Rol oldRole = cliente.getUsuario().getRol();

                // Solo actualizar si cambió
                if (!oldRole.equals(updatedRole)) {
                    cliente.getUsuario().setRol(updatedRole);
                    clienteRepository.save(cliente);

                    logger.info("Updated role for user {} from {} to {}",
                            jwt.getSubject(), oldRole, updatedRole);

                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "oldRole", oldRole.name(),
                            "newRole", updatedRole.name(),
                            "message", "Rol actualizado exitosamente"
                    ));
                } else {
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "currentRole", updatedRole.name(),
                            "message", "El rol ya está actualizado"
                    ));
                }
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Usuario no encontrado en la base de datos"
                ));
            }

        } catch (Exception e) {
            logger.error("Error refreshing roles for user {}: {}",
                    jwt != null ? jwt.getSubject() : "unknown", e.getMessage());

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Error refrescando roles: " + e.getMessage()
            ));
        }
    }

    /**
     * Verifica los roles actuales (para debugging)
     */
    @GetMapping("/current-roles")
    public ResponseEntity<?> getCurrentRoles(@AuthenticationPrincipal Jwt jwt) {
        try {
            if (jwt == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Token requerido"));
            }

            // Definir constante local
            final String ROLES_CLAIM = "https://APIElBuenSabor/roles";

            // Roles del token actual
            Object tokenRoles = jwt.getClaim(ROLES_CLAIM);
            Rol extractedRole = extractRoleFromJwt(jwt);

            // Rol en BD local
            Optional<Cliente> clienteOpt = clienteRepository.findByUsuarioAuth0Id(jwt.getSubject());
            Rol dbRole = clienteOpt.map(c -> c.getUsuario().getRol()).orElse(null);

            return ResponseEntity.ok(Map.of(
                    "tokenRoles", tokenRoles,
                    "extractedRole", extractedRole.name(),
                    "dbRole", dbRole != null ? dbRole.name() : "null",
                    "rolesMatch", extractedRole.equals(dbRole),
                    "userId", jwt.getSubject()
            ));

        } catch (Exception e) {
            logger.error("Error getting current roles: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/debug-user")
    public ResponseEntity<?> debugUser(@AuthenticationPrincipal Jwt jwt) {
        try {
            String auth0Id = jwt.getSubject();
            final String ROLES_CLAIM = "https://APIElBuenSabor/roles";
            Object tokenRoles = jwt.getClaim(ROLES_CLAIM);
            Rol tokenRole = extractRoleFromJwt(jwt);

            Optional<Cliente> clienteOpt = clienteRepository.findByUsuarioAuth0Id(auth0Id);
            String dbRole = clienteOpt.map(c -> c.getUsuario().getRol().name()).orElse("NOT_FOUND");

            return ResponseEntity.ok(Map.of(
                    "auth0Id", auth0Id,
                    "tokenRoles", tokenRoles,
                    "extractedTokenRole", tokenRole.name(),
                    "databaseRole", dbRole,
                    "rolesMatch", tokenRole.name().equals(dbRole)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}