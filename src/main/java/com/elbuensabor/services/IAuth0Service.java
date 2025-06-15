package com.elbuensabor.services;

import com.elbuensabor.dto.response.LoginResponseDTO;
import com.elbuensabor.entities.Cliente;
import com.elbuensabor.entities.Rol;
import org.springframework.security.oauth2.jwt.Jwt;

public interface IAuth0Service {

    /**
     * Procesa un usuario autenticado con Auth0 y retorna la respuesta de login
     */
    LoginResponseDTO processAuth0User(Jwt jwt);

    /**
     * Busca o crea un cliente basado en datos de Auth0
     */
    Cliente findOrCreateClienteFromAuth0(String auth0Id, String email, String nombre, String apellido, Rol rol);

    /**
     * Verifica si un token es de Auth0 o local
     */
    boolean isAuth0Token(String token);
}