package com.elbuensabor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacturaResponseDTO {

    private Long idFactura;
    private LocalDate fechaFactura;
    private String nroComprobante;
    private Double subTotal;
    private Double descuento;
    private Double gastosEnvio;
    private Double totalVenta;

    // Información del pedido asociado
    private Long pedidoId;
    private String estadoPedido;
    private String tipoEnvio;

    // Información del cliente
    private Long clienteId;
    private String nombreCliente;
    private String apellidoCliente;

    // Información de pagos
    private List<PagoSummaryDTO> pagos;
    private Double totalPagado;
    private Double saldoPendiente;
    private Boolean completamentePagada;
}