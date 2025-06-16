package com.elbuensabor.services;

<<<<<<< HEAD
import com.elbuensabor.dto.request.ClienteRegisterDTO;
import com.elbuensabor.dto.response.ClienteResponseDTO;
import com.elbuensabor.entities.Cliente;
import org.springframework.security.oauth2.jwt.Jwt;

public interface IAuth0Service {

    ClienteResponseDTO registerClienteWithAuth0(ClienteRegisterDTO registerDTO, String auth0Id);

    Cliente getOrCreateClienteFromJwt(Jwt jwt);

    Cliente getClienteByEmail(String email);

    String extractEmailFromJwt(Jwt jwt);

    String extractAuth0IdFromJwt(Jwt jwt);
=======
import com.elbuensabor.dto.response.LoginResponseDTO;
import com.elbuensabor.entities.Cliente;
import com.elbuensabor.entities.Rol;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;

public interface IAuth0Service {

    /**
     * Procesa un usuario autenticado con Auth0 y retorna la respuesta de login
     */

    LoginResponseDTO processAuth0User(Jwt jwt, Map<String, Object> userData);
    /**
     * Busca o crea un cliente basado en datos de Auth0
     */
    Cliente findOrCreateClienteFromAuth0(String auth0Id, String email, String nombre, String apellido, Rol rol);

    /**
     * Verifica si un token es de Auth0 o local
     */
    boolean isAuth0Token(String token);
>>>>>>> ramaLucho
}