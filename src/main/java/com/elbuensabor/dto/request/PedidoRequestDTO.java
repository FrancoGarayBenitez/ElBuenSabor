package com.elbuensabor.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoRequestDTO {

    @NotNull(message = "El cliente es obligatorio")
    private Long idCliente;

    @NotNull(message = "El tipo de envío es obligatorio")
    private String tipoEnvio; // "DELIVERY" o "TAKE_AWAY"

    private Long idDomicilio; // Solo si es DELIVERY

    @Valid
    @NotEmpty(message = "El pedido debe tener al menos un producto")
    private List<DetallePedidoRequestDTO> detalles;

    private String observaciones;
}