package com.elbuensabor.controllers;

import com.elbuensabor.entities.Cliente;
import com.elbuensabor.services.ClienteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping("/buscar-email")
    public Cliente buscarPorEmail(@RequestParam String email) {
        return clienteService.findByEmail(email).orElseThrow();
    }

    @PostMapping
    public Cliente guardar(@RequestBody Cliente cliente) {
        return clienteService.save(cliente);
    }

    @GetMapping
    public List<Cliente> obtenerTodos() {
        return clienteService.findAll();
    }

    @GetMapping("/{id}")
    public Cliente obtenerPorId(@PathVariable Long id) {
        return clienteService.findById(id).orElseThrow();
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        clienteService.deleteById(id);
    }
}
