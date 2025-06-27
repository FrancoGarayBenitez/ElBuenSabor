package com.elbuensabor.controllers;

import com.elbuensabor.dto.request.ClientePerfilDTO;
import com.elbuensabor.dto.response.ClienteResponseDTO;
import com.elbuensabor.services.IClienteService;
import com.elbuensabor.services.mapper.ClientePerfilMapper;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

/**
 * Controlador para operaciones CRUD de clientes
 * El registro se maneja en Auth0Controller
 *
 * Incluye funcionalidades específicas de perfil y cambio de contraseña
 */
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private static final Logger logger = LoggerFactory.getLogger(ClienteController.class);
    private final IClienteService clienteService;
    private final ClientePerfilMapper clientePerfilMapper;

    @Value("${auth0.domain:dev-ik2kub20ymu4sfpr.us.auth0.com}")
    private String auth0Domain;

    @Value("${auth0.client-id:4u4F4fKQrsD9Bvvh9ODZ0tnqzR431TBV}")
    private String clientId;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    public ClienteController(IClienteService clienteService, ClientePerfilMapper clientePerfilMapper) {
        this.clienteService = clienteService;
        this.clientePerfilMapper = clientePerfilMapper;
    }

    // ==================== ENDPOINTS ADMINISTRATIVOS ====================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClienteResponseDTO>> getAllClientes() {
        logger.debug("Admin requesting all clientes");
        List<ClienteResponseDTO> clientes = clienteService.findAll();
        return ResponseEntity.ok(clientes);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @clienteSecurityService.isOwner(authentication.name, #id)")
    public ResponseEntity<ClienteResponseDTO> getClienteById(@PathVariable Long id) {
        logger.debug("Getting cliente with ID: {}", id);
        ClienteResponseDTO cliente = clienteService.findById(id);
        return ResponseEntity.ok(cliente);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @clienteSecurityService.isOwner(authentication.name, #id)")
    public ResponseEntity<ClienteResponseDTO> updateCliente(@PathVariable Long id,
                                                            @Valid @RequestBody ClienteResponseDTO clienteDTO) {
        logger.debug("Updating cliente with ID: {}", id);
        ClienteResponseDTO clienteActualizado = clienteService.update(id, clienteDTO);
        return ResponseEntity.ok(clienteActualizado);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCliente(@PathVariable Long id) {
        logger.info("Admin deleting cliente with ID: {}", id);
        clienteService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== ENDPOINTS DE PERFIL ====================

    /**
     * GET /api/clientes/perfil
     * Obtiene el perfil completo del usuario autenticado (incluye domicilios)
     */
    @GetMapping("/perfil")
    public ResponseEntity<ClienteResponseDTO> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        logger.debug("Getting complete profile for Auth0 user: {}", jwt.getSubject());
        ClienteResponseDTO cliente = clienteService.findByAuth0Id(jwt.getSubject());
        return ResponseEntity.ok(cliente);
    }

    /**
     * GET /api/clientes/perfil/info
     * Obtiene solo la información personal del usuario (sin domicilios)
     * Útil para formularios de edición de perfil
     */
    @GetMapping("/perfil/info")
    public ResponseEntity<ClientePerfilDTO> getMyProfileInfo(@AuthenticationPrincipal Jwt jwt) {
        logger.debug("Getting profile info for Auth0 user: {}", jwt.getSubject());

        ClienteResponseDTO clienteCompleto = clienteService.findByAuth0Id(jwt.getSubject());
        ClientePerfilDTO perfilInfo = clientePerfilMapper.responseToPerfilDTO(clienteCompleto);

        return ResponseEntity.ok(perfilInfo);
    }

    /**
     * PUT /api/clientes/perfil/info
     * Actualiza solo la información personal del usuario (sin domicilios)
     * Los domicilios se manejan con el DomicilioPerfilController
     */
    @PutMapping("/perfil/info")
    public ResponseEntity<ClienteResponseDTO> updateMyProfileInfo(@Valid @RequestBody ClientePerfilDTO clientePerfilDTO,
                                                                  @AuthenticationPrincipal Jwt jwt) {
        logger.debug("Updating profile info for Auth0 user: {}", jwt.getSubject());

        // Buscar cliente actual
        ClienteResponseDTO currentCliente = clienteService.findByAuth0Id(jwt.getSubject());

        // Crear DTO para actualización manteniendo email y domicilios
        ClienteResponseDTO clienteParaActualizar = new ClienteResponseDTO();
        clienteParaActualizar.setNombre(clientePerfilDTO.getNombre());
        clienteParaActualizar.setApellido(clientePerfilDTO.getApellido());
        clienteParaActualizar.setTelefono(clientePerfilDTO.getTelefono());
        clienteParaActualizar.setFechaNacimiento(clientePerfilDTO.getFechaNacimiento());
        clienteParaActualizar.setEmail(currentCliente.getEmail()); // Mantener email actual
        clienteParaActualizar.setImagen(clientePerfilDTO.getImagen());
        // Los domicilios se ignoran en el servicio

        // Actualizar usando el ID local
        ClienteResponseDTO clienteActualizado = clienteService.update(currentCliente.getIdCliente(), clienteParaActualizar);

        logger.info("Profile info updated successfully for user: {}", jwt.getSubject());
        return ResponseEntity.ok(clienteActualizado);
    }

    /**
     * PUT /api/clientes/perfil
     * Actualiza el perfil completo del usuario (DEPRECATED - usar /perfil/info para info personal)
     * Mantenido por compatibilidad
     */
    @PutMapping("/perfil")
    @Deprecated
    public ResponseEntity<ClienteResponseDTO> updateMyProfile(@Valid @RequestBody ClienteResponseDTO clienteDTO,
                                                              @AuthenticationPrincipal Jwt jwt) {
        logger.debug("Updating complete profile for Auth0 user: {} (DEPRECATED endpoint)", jwt.getSubject());

        // Buscar cliente por Auth0 ID y obtener su ID local
        ClienteResponseDTO currentCliente = clienteService.findByAuth0Id(jwt.getSubject());

        // Actualizar usando el ID local
        ClienteResponseDTO clienteActualizado = clienteService.update(currentCliente.getIdCliente(), clienteDTO);
        return ResponseEntity.ok(clienteActualizado);
    }

    /**
     * DELETE /api/clientes/perfil
     * Elimina la cuenta del usuario autenticado
     */
    @DeleteMapping("/perfil")
    public ResponseEntity<Void> deleteMyProfile(@AuthenticationPrincipal Jwt jwt) {
        logger.info("User {} requesting account deletion", jwt.getSubject());

        ClienteResponseDTO cliente = clienteService.findByAuth0Id(jwt.getSubject());
        clienteService.delete(cliente.getIdCliente());

        logger.info("Cliente account deleted successfully for user: {}", jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/clientes/perfil/estadisticas
     * Obtiene estadísticas del perfil del usuario
     */
    @GetMapping("/perfil/estadisticas")
    public ResponseEntity<Map<String, Object>> getMyProfileStats(@AuthenticationPrincipal Jwt jwt) {
        logger.debug("Getting profile statistics for Auth0 user: {}", jwt.getSubject());

        ClienteResponseDTO cliente = clienteService.findByAuth0Id(jwt.getSubject());

        Map<String, Object> estadisticas = Map.of(
                "idCliente", cliente.getIdCliente(),
                "nombreCompleto", cliente.getNombre() + " " + cliente.getApellido(),
                "email", cliente.getEmail(),
                "cantidadDomicilios", cliente.getDomicilios() != null ? cliente.getDomicilios().size() : 0,
                "tieneImagen", cliente.getImagen() != null,
                "fechaNacimiento", cliente.getFechaNacimiento()
        );

        return ResponseEntity.ok(estadisticas);
    }

    // ==================== ENDPOINTS DE SEGURIDAD ====================

    /**
     * GET /api/clientes/perfil/auth0-config
     * Obtiene información para redirigir a Auth0 para cambio de contraseña
     */
    @GetMapping("/perfil/auth0-config")
    public ResponseEntity<Map<String, String>> getAuth0Config(@AuthenticationPrincipal Jwt jwt) {
        logger.debug("Getting Auth0 config for user: {}", jwt.getSubject());

        Map<String, String> auth0Config = Map.of(
                "changePasswordUrl", "https://dev-ik2kub20ymu4sfpr.us.auth0.com/login?screen_hint=forgot_password",
                "manageAccountUrl", "https://dev-ik2kub20ymu4sfpr.us.auth0.com/u/profile",
                "userSubject", jwt.getSubject(),
                "loginUrl", "https://dev-ik2kub20ymu4sfpr.us.auth0.com/login"
        );

        return ResponseEntity.ok(auth0Config);
    }

    /**
     * POST /api/clientes/perfil/password-reset
     * Envía email de recuperación de contraseña a través de Auth0
     */
    @PostMapping("/perfil/password-reset")
    public ResponseEntity<Map<String, Object>> requestPasswordReset(@AuthenticationPrincipal Jwt jwt) {
        logger.info("Password reset requested for user: {}", jwt.getSubject());

        try {
            // Obtener email del custom claim correcto
            String email = jwt.getClaimAsString("https://APIElBuenSabor/email");

            // Fallback a claim estándar si no existe el custom claim
            if (email == null || email.trim().isEmpty()) {
                email = jwt.getClaimAsString("email");
            }

            // Si aún no tenemos email, buscar en el cliente de la base de datos
            if (email == null || email.trim().isEmpty()) {
                try {
                    ClienteResponseDTO cliente = clienteService.findByAuth0Id(jwt.getSubject());
                    email = cliente.getEmail();
                } catch (Exception e) {
                    logger.error("Error finding cliente in database: {}", e.getMessage());
                }
            }

            if (email == null || email.trim().isEmpty()) {
                logger.error("Could not obtain email for user: {}", jwt.getSubject());
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "No se pudo obtener el email del usuario"
                        ));
            }

            // Preparar request a Auth0
            String url = String.format("https://%s/dbconnections/change_password", auth0Domain);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = Map.of(
                    "client_id", clientId,
                    "email", email,
                    "connection", "Username-Password-Authentication"
            );

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> auth0Response = restTemplate.postForEntity(url, request, String.class);

            if (auth0Response.getStatusCode() == HttpStatus.OK) {
                String responseBody = auth0Response.getBody();

                // Verificar si hay error en la respuesta
                if (responseBody != null && responseBody.contains("error")) {
                    logger.warn("Auth0 returned error in response body: {}", responseBody);
                    return ResponseEntity.ok(Map.of(
                            "success", false,
                            "message", "Error de Auth0: " + responseBody,
                            "email", email
                    ));
                }

                logger.info("Password reset email sent successfully for user: {}", email);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Se ha enviado un email con instrucciones para cambiar tu contraseña",
                        "email", email
                ));
            } else {
                logger.warn("Auth0 password reset failed with status: {}", auth0Response.getStatusCode());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(
                                "success", false,
                                "message", "Error al procesar la solicitud de cambio de contraseña"
                        ));
            }

        } catch (Exception e) {
            logger.error("Error requesting password reset for user {}: {}", jwt.getSubject(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error interno del servidor"
                    ));
        }
    }

    /**
     * GET /api/clientes/perfil/auth0-info
     * Obtiene información básica del perfil desde Auth0
     */
    @GetMapping("/perfil/auth0-info")
    public ResponseEntity<Map<String, Object>> getAuth0Info(@AuthenticationPrincipal Jwt jwt) {
        logger.debug("Getting Auth0 info for user: {}", jwt.getSubject());

        try {
            // Obtener email del custom claim correcto
            String email = jwt.getClaimAsString("https://APIElBuenSabor/email");
            if (email == null || email.trim().isEmpty()) {
                email = jwt.getClaimAsString("email");
            }

            // Detectar tipo de proveedor basado en el subject
            String provider = "username-password"; // Por defecto
            if (jwt.getSubject().startsWith("google-oauth2|")) {
                provider = "google-oauth2";
            }

            Map<String, Object> auth0Info = Map.of(
                    "sub", jwt.getSubject(),
                    "email", email != null ? email : "",
                    "email_verified", jwt.getClaimAsBoolean("email_verified") != null ? jwt.getClaimAsBoolean("email_verified") : false,
                    "name", jwt.getClaimAsString("name") != null ? jwt.getClaimAsString("name") : "",
                    "picture", jwt.getClaimAsString("picture") != null ? jwt.getClaimAsString("picture") : "",
                    "updated_at", jwt.getClaimAsString("updated_at") != null ? jwt.getClaimAsString("updated_at") : "",
                    "provider", provider
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", auth0Info
            ));

        } catch (Exception e) {
            logger.error("Error getting Auth0 info for user {}: {}", jwt.getSubject(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al obtener información de Auth0"
                    ));
        }
    }
}