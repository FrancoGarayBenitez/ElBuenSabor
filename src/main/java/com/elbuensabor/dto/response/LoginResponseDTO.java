package com.elbuensabor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;
    private String tipo; // "Bearer"
    private String email;
    private String rol;
    private Long userId;
    private String mensaje;

    // Constructor para login exitoso
    public LoginResponseDTO(String token, String email, String rol, Long userId) {
        this.token = token;
        this.tipo = "Bearer";
        this.email = email;
        this.rol = rol;
        this.userId = userId;
        this.mensaje = "Login exitoso";
    }
}
