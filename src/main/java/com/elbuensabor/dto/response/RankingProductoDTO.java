package com.elbuensabor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RankingProductoDTO {
    private Long idArticulo;
    private String denominacionArticulo;
    private Long cantidadVendida;
}
