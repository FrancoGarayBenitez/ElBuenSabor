package com.elbuensabor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetallePedidoResponseDTO {
    private Long idDetallePedido;
    private Long idArticulo;
    private String denominacionArticulo;
    private Integer cantidad;
    private Double precioUnitario;
    private Double subtotal;
    private String unidadMedida;
    private Integer tiempoPreparacion; // minutos
}
