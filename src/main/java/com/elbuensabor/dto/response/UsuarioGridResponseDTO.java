package com.elbuensabor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UsuarioGridResponseDTO {
    private Long idUsuario;
    private String email;
    private String rol;
    private String nombre;
    private String apellido;
}