package com.elbuensabor.controllers;

import com.elbuensabor.dto.response.FacturaResponseDTO;
import com.elbuensabor.services.IFacturaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/facturas")
public class FacturaController {

    @Autowired
    private IFacturaService facturaService;

    // ==================== ENDPOINTS BÁSICOS ====================

    @GetMapping
    public ResponseEntity<List<FacturaResponseDTO>> getAllFacturas() {
        List<FacturaResponseDTO> facturas = facturaService.findAll();
        return ResponseEntity.ok(facturas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacturaResponseDTO> getFacturaById(@PathVariable Long id) {
        FacturaResponseDTO factura = facturaService.findById(id);
        return ResponseEntity.ok(factura);
    }

    // ==================== BÚSQUEDAS ESPECÍFICAS ====================

    @GetMapping("/pedido/{pedidoId}")
    public ResponseEntity<FacturaResponseDTO> getFacturaByPedido(@PathVariable Long pedidoId) {
        FacturaResponseDTO factura = facturaService.findByPedidoId(pedidoId);
        return ResponseEntity.ok(factura);
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<FacturaResponseDTO>> getFacturasByCliente(@PathVariable Long clienteId) {
        List<FacturaResponseDTO> facturas = facturaService.findByClienteId(clienteId);
        return ResponseEntity.ok(facturas);
    }

    @GetMapping("/fecha-rango")
    public ResponseEntity<List<FacturaResponseDTO>> getFacturasByFechaRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        List<FacturaResponseDTO> facturas = facturaService.findByFechaRange(fechaInicio, fechaFin);
        return ResponseEntity.ok(facturas);
    }

    @GetMapping("/pendientes-pago")
    public ResponseEntity<List<FacturaResponseDTO>> getFacturasPendientesPago() {
        List<FacturaResponseDTO> facturas = facturaService.findFacturasPendientesPago();
        return ResponseEntity.ok(facturas);
    }

    // ==================== OPERACIONES ESPECÍFICAS ====================

    @GetMapping("/exists/pedido/{pedidoId}")
    public ResponseEntity<Boolean> existeFacturaParaPedido(@PathVariable Long pedidoId) {
        boolean existe = facturaService.existeFacturaParaPedido(pedidoId);
        return ResponseEntity.ok(existe);
    }

    @GetMapping("/generar-numero-comprobante")
    public ResponseEntity<String> generarNumeroComprobante() {
        String numeroComprobante = facturaService.generarNumeroComprobante();
        return ResponseEntity.ok(numeroComprobante);
    }
}