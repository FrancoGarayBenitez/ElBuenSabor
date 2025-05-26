package com.elbuensabor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaRequestDTO {
    @NotBlank(message = "La denominación es obligatoria")
    private String denominacion;

    @NotNull(message = "Debe especificar si es subcategoría")
    private Boolean esSubcategoria;

    // ID de la categoría padre (opcional, solo si esSubcategoria = true)
    private Long idCategoriaPadre;
}