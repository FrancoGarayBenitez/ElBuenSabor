package com.elbuensabor.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImagenDTO {
    private Long idImagen;
    private String denominacion;
    private String url;
}
