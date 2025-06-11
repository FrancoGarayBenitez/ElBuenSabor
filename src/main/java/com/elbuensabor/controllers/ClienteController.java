package com.elbuensabor.controllers;

import com.elbuensabor.dto.request.ClienteRegisterDTO;
import com.elbuensabor.dto.response.ClienteResponseDTO;
import com.elbuensabor.entities.Cliente;
import com.elbuensabor.services.IClienteService;
import com.elbuensabor.services.impl.Auth0ServiceImpl;
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
    private final Auth0ServiceImpl auth0Service;

    @Autowired
    public ClienteController(IClienteService clienteService, Auth0ServiceImpl auth0Service) {
        this.clienteService = clienteService;
        this.auth0Service = auth0Service;
    }

    // REGISTRO DE CLIENTE
    @PostMapping("/register")
    public ResponseEntity<ClienteResponseDTO> registerCliente(@Valid @RequestBody ClienteRegisterDTO registerDTO) {
        ClienteResponseDTO clienteRegistrado = clienteService.registerCliente(registerDTO);
        return new ResponseEntity<>(clienteRegistrado, HttpStatus.CREATED);
    }

    // OBTENER CLIENTE AUTENTICADO ACTUAL
    @GetMapping("/me")
    public ResponseEntity<ClienteResponseDTO> getCurrentCliente(@AuthenticationPrincipal Jwt jwt) {
        // Obtener o crear cliente desde el JWT de Auth0
        Cliente cliente = auth0Service.getOrCreateClienteFromJwt(jwt);
        ClienteResponseDTO clienteDTO = clienteService.findById(cliente.getIdCliente());
        return ResponseEntity.ok(clienteDTO);
    }

    // ACTUALIZAR PERFIL DEL CLIENTE AUTENTICADO
    @PutMapping("/me")
    public ResponseEntity<ClienteResponseDTO> updateCurrentCliente(
            @Valid @RequestBody ClienteResponseDTO clienteDTO,
            @AuthenticationPrincipal Jwt jwt) {

        // Obtener cliente actual
        Cliente cliente = auth0Service.getOrCreateClienteFromJwt(jwt);

        // Actualizar con los datos del DTO
        ClienteResponseDTO clienteActualizado = clienteService.update(cliente.getIdCliente(), clienteDTO);
        return ResponseEntity.ok(clienteActualizado);
    }

    // OPERACIONES CRUD GENÉRICAS
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
