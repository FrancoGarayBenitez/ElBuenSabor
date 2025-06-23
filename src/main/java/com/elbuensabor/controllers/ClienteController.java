package com.elbuensabor.controllers;

import com.elbuensabor.dto.response.ClienteResponseDTO;
import com.elbuensabor.services.IClienteService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private static final Logger logger = LoggerFactory.getLogger(ClienteController.class);
    private final IClienteService clienteService;

    public ClienteController(IClienteService clienteService) {
        this.clienteService = clienteService;
    }

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

    @GetMapping("/perfil")
    public ResponseEntity<ClienteResponseDTO> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        logger.debug("Getting profile for Auth0 user: {}", jwt.getSubject());
        ClienteResponseDTO cliente = clienteService.findByAuth0Id(jwt.getSubject());
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

    @PutMapping("/perfil")
    public ResponseEntity<ClienteResponseDTO> updateMyProfile(@Valid @RequestBody ClienteResponseDTO clienteDTO,
                                                              @AuthenticationPrincipal Jwt jwt) {
        logger.debug("Updating profile for Auth0 user: {}", jwt.getSubject());

        // Buscar cliente por Auth0 ID y obtener su ID local
        ClienteResponseDTO currentCliente = clienteService.findByAuth0Id(jwt.getSubject());

        // Actualizar usando el ID local
        ClienteResponseDTO clienteActualizado = clienteService.update(currentCliente.getIdCliente(), clienteDTO);
        return ResponseEntity.ok(clienteActualizado);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCliente(@PathVariable Long id) {
        logger.info("Admin deleting cliente with ID: {}", id);
        clienteService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/perfil")
    public ResponseEntity<Void> deleteMyProfile(@AuthenticationPrincipal Jwt jwt) {
        logger.info("User {} requesting account deletion", jwt.getSubject());

        ClienteResponseDTO cliente = clienteService.findByAuth0Id(jwt.getSubject());
        clienteService.delete(cliente.getIdCliente());

        // TODO: Implementar eliminación en Auth0 usando Management API
        logger.warn("Cliente deleted locally, but Auth0 user still exists. Consider implementing Auth0 Management API.");

        return ResponseEntity.noContent().build();
    }
}