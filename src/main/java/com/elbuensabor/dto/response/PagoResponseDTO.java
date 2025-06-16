package com.elbuensabor.dto.response;

import com.elbuensabor.entities.EstadoPago;
import com.elbuensabor.entities.FormaPago;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoResponseDTO {

    private Long idPago;
    private Long facturaId;
    private FormaPago formaPago;
    private EstadoPago estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private Double monto;
    private String moneda;
    private String descripcion;

    // Datos de Mercado Pago (si aplica)
    private DatosMercadoPagoDTO datosMercadoPago;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatosMercadoPagoDTO {
        private Long paymentId;
        private String status;
        private String statusDetail;
        private String paymentMethodId;
        private String paymentTypeId;
        private LocalDateTime dateCreated;
        private LocalDateTime dateApproved;
    }
}