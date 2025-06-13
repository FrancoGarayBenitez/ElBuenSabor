package com.elbuensabor.controllers;

import com.elbuensabor.dto.request.PedidoRequestDTO;
import com.elbuensabor.dto.response.PedidoResponseDTO;
import com.elbuensabor.services.IPedidoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")

public class PedidoController {

    private final IPedidoService pedidoService;

    @Autowired
    public PedidoController(IPedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // ==================== CREAR PEDIDO ====================
    @PostMapping
    public ResponseEntity<PedidoResponseDTO> crearPedido(@Valid @RequestBody PedidoRequestDTO pedidoRequest) {
        PedidoResponseDTO pedidoCreado = pedidoService.crearPedido(pedidoRequest);
        return new ResponseEntity<>(pedidoCreado, HttpStatus.CREATED);
    }

    // ==================== OBTENER PEDIDOS ====================
    @GetMapping
    public ResponseEntity<List<PedidoResponseDTO>> getAllPedidos() {
        List<PedidoResponseDTO> pedidos = pedidoService.findAll();
        return ResponseEntity.ok(pedidos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> getPedidoById(@PathVariable Long id) {
        PedidoResponseDTO pedido = pedidoService.findById(id);
        return ResponseEntity.ok(pedido);
    }

    @GetMapping("/cliente/{idCliente}")
    public ResponseEntity<List<PedidoResponseDTO>> getPedidosByCliente(@PathVariable Long idCliente) {
        List<PedidoResponseDTO> pedidos = pedidoService.findByCliente(idCliente);
        return ResponseEntity.ok(pedidos);
    }

    // ==================== OPERACIONES DE ESTADO ====================
    @PutMapping("/{id}/confirmar")
    public ResponseEntity<PedidoResponseDTO> confirmarPedido(@PathVariable Long id) {
        PedidoResponseDTO pedidoConfirmado = pedidoService.confirmarPedido(id);
        return ResponseEntity.ok(pedidoConfirmado);
    }

    @PutMapping("/{id}/preparacion")
    public ResponseEntity<PedidoResponseDTO> marcarEnPreparacion(@PathVariable Long id) {
        PedidoResponseDTO pedido = pedidoService.marcarEnPreparacion(id);
        return ResponseEntity.ok(pedido);
    }

    @PutMapping("/{id}/listo")
    public ResponseEntity<PedidoResponseDTO> marcarListo(@PathVariable Long id) {
        PedidoResponseDTO pedido = pedidoService.marcarListo(id);
        return ResponseEntity.ok(pedido);
    }

    @PutMapping("/{id}/entregado")
    public ResponseEntity<PedidoResponseDTO> marcarEntregado(@PathVariable Long id) {
        PedidoResponseDTO pedido = pedidoService.marcarEntregado(id);
        return ResponseEntity.ok(pedido);
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<PedidoResponseDTO> cancelarPedido(@PathVariable Long id) {
        PedidoResponseDTO pedido = pedidoService.cancelarPedido(id);
        return ResponseEntity.ok(pedido);
    }

    // ==================== VALIDACIONES PREVIAS ====================
    @PostMapping("/validar")
    public ResponseEntity<Boolean> validarPedido(@Valid @RequestBody PedidoRequestDTO pedidoRequest) {
        Boolean esValido = pedidoService.validarStockDisponible(pedidoRequest);
        return ResponseEntity.ok(esValido);
    }

    @PostMapping("/calcular-total")
    public ResponseEntity<Double> calcularTotal(@Valid @RequestBody PedidoRequestDTO pedidoRequest) {
        Double total = pedidoService.calcularTotal(pedidoRequest);
        return ResponseEntity.ok(total);
    }

    @PostMapping("/tiempo-estimado")
    public ResponseEntity<Integer> calcularTiempoEstimado(@Valid @RequestBody PedidoRequestDTO pedidoRequest) {
        Integer tiempoMinutos = pedidoService.calcularTiempoEstimado(pedidoRequest);
        return ResponseEntity.ok(tiempoMinutos);
    }

    // ==================== FILTROS PARA DIFERENTES ROLES ====================
    @GetMapping("/pendientes")
    public ResponseEntity<List<PedidoResponseDTO>> getPedidosPendientes() {
        List<PedidoResponseDTO> pedidos = pedidoService.findPedidosPendientes();
        return ResponseEntity.ok(pedidos);
    }

    @GetMapping("/en-preparacion")
    public ResponseEntity<List<PedidoResponseDTO>> getPedidosEnPreparacion() {
        List<PedidoResponseDTO> pedidos = pedidoService.findPedidosEnPreparacion();
        return ResponseEntity.ok(pedidos);
    }

    @GetMapping("/listos-para-entrega")
    public ResponseEntity<List<PedidoResponseDTO>> getPedidosListosParaEntrega() {
        List<PedidoResponseDTO> pedidos = pedidoService.findPedidosListosParaEntrega();
        return ResponseEntity.ok(pedidos);
    }
}