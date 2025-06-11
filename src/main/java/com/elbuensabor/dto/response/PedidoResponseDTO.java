package com.elbuensabor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoResponseDTO {
    private Long idPedido;
    private LocalDateTime fecha;
    private LocalTime horaEstimadaFinalizacion;
    private Double total;
    private String estado;
    private String tipoEnvio;

    // Información del cliente
    private Long idCliente;
    private String nombreCliente;
    private String telefonoCliente;

    // Información del domicilio (si es delivery)
    private DomicilioResponseDTO domicilio;

    // Detalles del pedido
    private List<DetallePedidoResponseDTO> detalles;

    // Información adicional
    private Integer tiempoEstimadoTotal; // en minutos
    private Boolean stockSuficiente;
}