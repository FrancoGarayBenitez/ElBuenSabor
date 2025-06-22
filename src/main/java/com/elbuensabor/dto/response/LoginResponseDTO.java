package com.elbuensabor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para login con Auth0
 * Contiene información del usuario autenticado y el token de Auth0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {

    private String token;        // Token JWT de Auth0
    private String tipo;         // Siempre "Bearer"
    private String email;        // Email del usuario
    private String rol;          // Rol del usuario (CLIENTE, ADMIN, etc.)
    private Long userId;         // ID local del usuario
    private String nombre;       // Nombre del cliente
    private String apellido;     // Apellido del cliente
    private String mensaje;      // Mensaje de éxito

    /**
     * Constructor principal para login exitoso con Auth0
     */
    public LoginResponseDTO(String token, String email, String rol, Long userId, String nombre, String apellido) {
        this.token = token;
        this.tipo = "Bearer";
        this.email = email;
        this.rol = rol;
        this.userId = userId;
        this.nombre = nombre;
        this.apellido = apellido;
        this.mensaje = "Login exitoso con Auth0";
    }

    /**
     * Constructor para respuestas de error
     */
    public LoginResponseDTO(String mensaje) {
        this.mensaje = mensaje;
        this.tipo = "Bearer";
    }
}