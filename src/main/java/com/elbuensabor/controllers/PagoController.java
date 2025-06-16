package com.elbuensabor.controllers;

import com.elbuensabor.dto.request.MercadoPagoPreferenceDTO;
import com.elbuensabor.dto.request.PagoRequestDTO;
import com.elbuensabor.dto.response.MercadoPagoPreferenceResponseDTO;
import com.elbuensabor.dto.response.PagoResponseDTO;
import com.elbuensabor.entities.EstadoPago;
import com.elbuensabor.entities.FormaPago;
import com.elbuensabor.services.IMercadoPagoService;
import com.elbuensabor.services.IPagoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pagos")

public class PagoController {

    @Autowired
    private IPagoService pagoService;

    @Autowired
    private IMercadoPagoService mercadoPagoService;

    @PostMapping
    public ResponseEntity<PagoResponseDTO> crearPago(@Valid @RequestBody PagoRequestDTO pagoRequestDTO) {
        PagoResponseDTO pago = pagoService.crearPago(pagoRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(pago);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagoResponseDTO> getPagoById(@PathVariable Long id) {
        PagoResponseDTO pago = pagoService.findById(id);
        return ResponseEntity.ok(pago);
    }

    @GetMapping
    public ResponseEntity<List<PagoResponseDTO>> getAllPagos() {
        List<PagoResponseDTO> pagos = pagoService.findAll();
        return ResponseEntity.ok(pagos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PagoResponseDTO> updatePago(@PathVariable Long id, @Valid @RequestBody PagoResponseDTO pagoDTO) {
        PagoResponseDTO pagoActualizado = pagoService.update(id, pagoDTO);
        return ResponseEntity.ok(pagoActualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePago(@PathVariable Long id) {
        pagoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/factura/{facturaId}")
    public ResponseEntity<List<PagoResponseDTO>> getPagosByFactura(@PathVariable Long facturaId) {
        List<PagoResponseDTO> pagos = pagoService.getPagosByFactura(facturaId);
        return ResponseEntity.ok(pagos);
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<PagoResponseDTO>> getPagosByEstado(@PathVariable EstadoPago estado) {
        List<PagoResponseDTO> pagos = pagoService.getPagosByEstado(estado);
        return ResponseEntity.ok(pagos);
    }

    @GetMapping("/forma-pago/{formaPago}")
    public ResponseEntity<List<PagoResponseDTO>> getPagosByFormaPago(@PathVariable FormaPago formaPago) {
        List<PagoResponseDTO> pagos = pagoService.getPagosByFormaPago(formaPago);
        return ResponseEntity.ok(pagos);
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<PagoResponseDTO> actualizarEstadoPago(
            @PathVariable Long id,
            @RequestBody Map<String, EstadoPago> request) {
        EstadoPago nuevoEstado = request.get("estado");
        PagoResponseDTO pago = pagoService.actualizarEstadoPago(id, nuevoEstado);
        return ResponseEntity.ok(pago);
    }

    @PostMapping("/{id}/crear-preferencia-mp")
    public ResponseEntity<MercadoPagoPreferenceResponseDTO> crearPreferenciaMercadoPago(
            @PathVariable Long id,
            @RequestBody MercadoPagoPreferenceDTO preferenceDTO) {
        try {
            // Agregar referencia externa con el ID del pago
            preferenceDTO.setExternalReference("PAGO_" + id);

            MercadoPagoPreferenceResponseDTO preference = mercadoPagoService.crearPreferencia(preferenceDTO);

            // Actualizar el pago con el preference ID
            pagoService.procesarPagoMercadoPago(id, preference.getId());

            return ResponseEntity.ok(preference);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Webhook para Mercado Pago
    @PostMapping("/webhook/mercadopago")
    public ResponseEntity<String> webhookMercadoPago(@RequestBody Map<String, Object> notification) {
        try {
            if ("payment".equals(notification.get("type"))) {
                Map<String, Object> data = (Map<String, Object>) notification.get("data");
                Long paymentId = Long.valueOf(data.get("id").toString());

                // Aquí deberías hacer una consulta a la API de MP para obtener los detalles completos
                // Por ahora simulamos la confirmación
                pagoService.confirmarPagoMercadoPago(paymentId, "approved", "accredited");
            }
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error procesando webhook");
        }
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<PagoResponseDTO> cancelarPago(@PathVariable Long id) {
        PagoResponseDTO pago = pagoService.cancelarPago(id);
        return ResponseEntity.ok(pago);
    }

    @PutMapping("/{id}/reembolsar")
    public ResponseEntity<PagoResponseDTO> procesarReembolso(@PathVariable Long id) {
        PagoResponseDTO pago = pagoService.procesarReembolso(id);
        return ResponseEntity.ok(pago);
    }

    @GetMapping("/factura/{facturaId}/total-pagado")
    public ResponseEntity<Double> getTotalPagadoFactura(@PathVariable Long facturaId) {
        Double total = pagoService.getTotalPagadoFactura(facturaId);
        return ResponseEntity.ok(total);
    }

    @GetMapping("/factura/{facturaId}/saldo-pendiente")
    public ResponseEntity<Double> getSaldoPendienteFactura(@PathVariable Long facturaId) {
        Double saldo = pagoService.getSaldoPendienteFactura(facturaId);
        return ResponseEntity.ok(saldo);
    }

    @GetMapping("/factura/{facturaId}/completamente-pagada")
    public ResponseEntity<Boolean> isFacturaCompletamentePagada(@PathVariable Long facturaId) {
        Boolean completamentePagada = pagoService.isFacturaCompletamentePagada(facturaId);
        return ResponseEntity.ok(completamentePagada);
    }
}