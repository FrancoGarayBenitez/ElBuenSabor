package com.elbuensabor.controllers;

import com.elbuensabor.dto.request.PedidoRequestDTO;
import com.elbuensabor.dto.response.HorarioStatusResponseDTO;
import com.elbuensabor.dto.response.PedidoResponseDTO;
import com.elbuensabor.services.IHorarioService;
import com.elbuensabor.services.IPedidoService;
import com.elbuensabor.services.impl.HorarioServiceImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.elbuensabor.dto.response.FacturaResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final IPedidoService pedidoService;
    private final IHorarioService horarioService;
    private static final Logger logger = LoggerFactory.getLogger(PedidoController.class);

    @Autowired
    public PedidoController(IPedidoService pedidoService, IHorarioService horarioService) { // 3. USA LA INTERFAZ
        this.pedidoService = pedidoService;
        this.horarioService = horarioService;
    }

    // ==================== CREAR PEDIDO ====================
    @PostMapping
    public ResponseEntity<PedidoResponseDTO> crearPedido(@Valid @RequestBody PedidoRequestDTO pedidoRequest) {
        PedidoResponseDTO pedidoCreado = pedidoService.crearPedido(pedidoRequest);
        return new ResponseEntity<>(pedidoCreado, HttpStatus.CREATED);
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

    // ==================== ENDPOINTS CON PATH VARIABLES ESPECÍFICOS ====================
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

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> getPedidoById(@PathVariable Long id) {
        PedidoResponseDTO pedido = pedidoService.findById(id);
        return ResponseEntity.ok(pedido);
    }

    // Para mostrador/caja - pedidos listos para retirar
    @GetMapping("/listos-para-retiro")
    public ResponseEntity<List<PedidoResponseDTO>> getPedidosListosParaRetiro() {
        List<PedidoResponseDTO> pedidos = pedidoService.findPedidosListosParaRetiro();
        return ResponseEntity.ok(pedidos);
    }

    // ==================== ENDPOINTS ESPECÍFICOS PARA COCINA ====================

    // Dashboard de cocina - pedidos que necesitan atención
    @GetMapping("/cocina/pendientes-confirmacion")
    public ResponseEntity<List<PedidoResponseDTO>> getPedidosPendientesConfirmacion() {
        List<PedidoResponseDTO> pedidos = pedidoService.findPedidosPendientes();
        return ResponseEntity.ok(pedidos);
    }

    @GetMapping("/cocina/en-proceso")
    public ResponseEntity<List<PedidoResponseDTO>> getPedidosEnProceso() {
        List<PedidoResponseDTO> pedidos = pedidoService.findPedidosEnPreparacion();
        return ResponseEntity.ok(pedidos);
    }

    // ==================== ENDPOINTS PARA DELIVERY ====================

    // Solo pedidos de delivery listos para entregar
    @GetMapping("/delivery/pendientes")
    public ResponseEntity<List<PedidoResponseDTO>> getPedidosDeliveryPendientes() {
        List<PedidoResponseDTO> pedidos = pedidoService.findPedidosListosParaEntrega();
        return ResponseEntity.ok(pedidos);
    }

    // ==================== ENDPOINTS PARA MOSTRADOR ====================

    // Solo pedidos de take away listos para retirar
    @GetMapping("/mostrador/listos")
    public ResponseEntity<List<PedidoResponseDTO>> getPedidosMostradorListos() {
        List<PedidoResponseDTO> pedidos = pedidoService.findPedidosListosParaRetiro();
        return ResponseEntity.ok(pedidos);
    }

    /**
     * Obtiene todos los pedidos listos (para vista de cocina)
     */
    @GetMapping("/listos")
    public ResponseEntity<List<PedidoResponseDTO>> getPedidosListos() {
        try {
            List<PedidoResponseDTO> pedidos = pedidoService.findPedidosListos();
            return ResponseEntity.ok(pedidos);
        } catch (Exception e) {
            logger.error("Error al obtener pedidos listos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtiene la factura asociada a un pedido específico
     */
    @GetMapping("/{id}/factura")
    public ResponseEntity<FacturaResponseDTO> getFacturaPedido(@PathVariable Long id) {
        try {
            FacturaResponseDTO factura = pedidoService.getFacturaPedido(id);
            return ResponseEntity.ok(factura);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtiene todos los pedidos (endpoint básico para gestión)
     */
    @GetMapping
    public ResponseEntity<List<PedidoResponseDTO>> getAllPedidos() {
        try {
            List<PedidoResponseDTO> pedidos = pedidoService.findAll();
            return ResponseEntity.ok(pedidos);
        } catch (Exception e) {
            logger.error("Error al obtener todos los pedidos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}