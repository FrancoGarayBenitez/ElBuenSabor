package com.elbuensabor.controllers;

import com.elbuensabor.dto.request.ClienteRegisterDTO;
import com.elbuensabor.dto.response.ClienteResponseDTO;
import com.elbuensabor.exceptions.ResourceNotFoundException;
import com.elbuensabor.services.IAuthService;
import com.elbuensabor.services.IClienteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final IClienteService clienteService;
    private final IAuthService authService;

    @Autowired
    public ClienteController(IClienteService clienteService, IAuthService authService) {
        this.clienteService = clienteService;
        this.authService = authService;
    }

    // ==================== 🎯 NUEVO ENDPOINT PARA MERCADOPAGO ====================

    /**
     * Obtener datos del cliente autenticado actual
     * Soporte para Auth0 y tokens locales
     */
    @GetMapping("/me")
    public ResponseEntity<ClienteResponseDTO> getClienteAutenticado(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @AuthenticationPrincipal Jwt jwt) {

        try {
            String email = null;

            // 🔍 ESTRATEGIA 1: Usuario de Auth0
            if (jwt != null) {
                email = jwt.getClaimAsString("email");
                System.out.println("📧 Email desde Auth0 JWT: " + email);
            }
            // 🔍 ESTRATEGIA 2: Token local
            else if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (authService.validateToken(token)) {
                    email = authService.extractEmailFromToken(token);
                    System.out.println("📧 Email desde token local: " + email);
                }
            }

            if (email == null || email.isEmpty()) {
                System.out.println("❌ No se pudo obtener email del usuario");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Buscar cliente por email
            ClienteResponseDTO cliente = clienteService.findByEmail(email);
            System.out.println("✅ Cliente encontrado: " + cliente.getNombre() + " " + cliente.getApellido());

            return ResponseEntity.ok(cliente);

        } catch (ResourceNotFoundException e) {
            System.out.println("❌ Cliente no encontrado: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            System.out.println("❌ Error en /clientes/me: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== ENDPOINTS EXISTENTES ====================

    @PostMapping("/register")
    public ResponseEntity<ClienteResponseDTO> registerCliente(@Valid @RequestBody ClienteRegisterDTO registerDTO) {
        ClienteResponseDTO clienteRegistrado = clienteService.registerCliente(registerDTO);
        return new ResponseEntity<>(clienteRegistrado, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ClienteResponseDTO>> getAllClientes() {
        List<ClienteResponseDTO> clientes = clienteService.findAll();
        return ResponseEntity.ok(clientes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> getClienteById(@PathVariable Long id) {
        ClienteResponseDTO cliente = clienteService.findById(id);
        return ResponseEntity.ok(cliente);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> updateCliente(
            @PathVariable Long id,
            @Valid @RequestBody ClienteResponseDTO clienteDTO) {
        ClienteResponseDTO clienteActualizado = clienteService.update(id, clienteDTO);
        return ResponseEntity.ok(clienteActualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCliente(@PathVariable Long id) {
        clienteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}