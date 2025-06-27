package com.elbuensabor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientosMonetariosDTO {
    private Double ingresos; // Total de ventas
    private Double costos;   // Suma de los costos de los productos vendidos
    private Double ganancias; // Ingresos - Costos
}
