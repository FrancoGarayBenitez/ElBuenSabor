package com.elbuensabor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {
    private String nombre;
    private String apellido;
    private String token;
    private String tipo; // "Bearer"
    private String email;
    private String rol;
    private Long userId;
    private String mensaje;

    // Constructor para login exitoso
    public LoginResponseDTO(String token, String email, String rol, Long userId, String nombre, String apellido) {
        this.token = token;
        this.tipo = "Bearer";
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.rol = rol;
        this.userId = userId;
        this.mensaje = "Login exitoso";
    }
}
