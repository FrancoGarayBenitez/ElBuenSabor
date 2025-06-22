package com.elbuensabor.controllers;

import com.elbuensabor.dto.response.ClienteResponseDTO;
import com.elbuensabor.services.IClienteService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador para operaciones CRUD de clientes
 * El registro se maneja ahora en Auth0Controller
 */
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private static final Logger logger = LoggerFactory.getLogger(ClienteController.class);
    private final IClienteService clienteService;

    @Autowired
    public ClienteController(IClienteService clienteService) {
        this.clienteService = clienteService;
    }

    /**
     * Obtiene todos los clientes - Solo para administradores
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClienteResponseDTO>> getAllClientes() {
        logger.debug("Admin requesting all clientes");
        List<ClienteResponseDTO> clientes = clienteService.findAll();
        return ResponseEntity.ok(clientes);
    }

    /**
     * Obtiene un cliente por ID
     * Los clientes solo pueden ver su propio perfil, los admins pueden ver cualquiera
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @clienteSecurityService.isOwner(authentication.name, #id)")
    public ResponseEntity<ClienteResponseDTO> getClienteById(@PathVariable Long id,
                                                             @AuthenticationPrincipal Jwt jwt) {
        logger.debug("Getting cliente with ID: {} for user: {}", id, jwt.getSubject());
        ClienteResponseDTO cliente = clienteService.findById(id);
        return ResponseEntity.ok(cliente);
    }

    /**
     * Obtiene el perfil del cliente autenticado actual
     */
    @GetMapping("/perfil")
    public ResponseEntity<ClienteResponseDTO> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        try {
            logger.debug("Getting profile for Auth0 user: {}", jwt.getSubject());

            // Buscar cliente por Auth0 ID
            ClienteResponseDTO cliente = clienteService.findByAuth0Id(jwt.getSubject());
            return ResponseEntity.ok(cliente);

        } catch (Exception e) {
            logger.error("Error getting profile for user {}: {}", jwt.getSubject(), e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Actualiza un cliente
     * Los clientes solo pueden actualizar su propio perfil, los admins pueden actualizar cualquiera
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @clienteSecurityService.isOwner(authentication.name, #id)")
    public ResponseEntity<ClienteResponseDTO> updateCliente(@PathVariable Long id,
                                                            @Valid @RequestBody ClienteResponseDTO clienteDTO,
                                                            @AuthenticationPrincipal Jwt jwt) {
        logger.debug("Updating cliente with ID: {} for user: {}", id, jwt.getSubject());
        ClienteResponseDTO clienteActualizado = clienteService.update(id, clienteDTO);
        return ResponseEntity.ok(clienteActualizado);
    }

    /**
     * Actualiza el perfil del cliente autenticado actual
     */
    @PutMapping("/perfil")
    public ResponseEntity<ClienteResponseDTO> updateMyProfile(@Valid @RequestBody ClienteResponseDTO clienteDTO,
                                                              @AuthenticationPrincipal Jwt jwt) {
        try {
            logger.debug("Updating profile for Auth0 user: {}", jwt.getSubject());

            // Buscar cliente por Auth0 ID y obtener su ID local
            ClienteResponseDTO currentCliente = clienteService.findByAuth0Id(jwt.getSubject());

            // Actualizar usando el ID local
            ClienteResponseDTO clienteActualizado = clienteService.update(currentCliente.getIdCliente(), clienteDTO);
            return ResponseEntity.ok(clienteActualizado);

        } catch (Exception e) {
            logger.error("Error updating profile for user {}: {}", jwt.getSubject(), e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Elimina un cliente - Solo para administradores
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCliente(@PathVariable Long id,
                                              @AuthenticationPrincipal Jwt jwt) {
        logger.info("Admin {} deleting cliente with ID: {}", jwt.getSubject(), id);
        clienteService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Elimina el perfil del cliente autenticado actual
     * Nota: También debería eliminar el usuario de Auth0, pero eso requiere Auth0 Management API
     */
    @DeleteMapping("/perfil")
    public ResponseEntity<Void> deleteMyProfile(@AuthenticationPrincipal Jwt jwt) {
        try {
            logger.info("User {} requesting account deletion", jwt.getSubject());

            // Buscar cliente por Auth0 ID
            ClienteResponseDTO cliente = clienteService.findByAuth0Id(jwt.getSubject());

            // Eliminar cliente local
            clienteService.delete(cliente.getIdCliente());

            // TODO: Implementar eliminación en Auth0 usando Management API
            logger.warn("Cliente deleted locally, but Auth0 user still exists. Consider implementing Auth0 Management API.");

            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            logger.error("Error deleting profile for user {}: {}", jwt.getSubject(), e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}