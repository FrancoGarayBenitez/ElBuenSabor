package com.elbuensabor.services;

import com.elbuensabor.dto.response.ClienteResponseDTO;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;

public interface IAuth0SyncService {

    /**
     * Sincroniza un usuario de Auth0 con nuestra base de datos
     * @param jwt Token JWT de Auth0
     * @return ClienteResponseDTO con la información del cliente
     */
    ClienteResponseDTO syncUserFromAuth0(Jwt jwt);

    /**
     * Obtiene la información del usuario actual desde la base de datos
     * @param jwt Token JWT de Auth0
     * @return ClienteResponseDTO con la información del cliente
     */
    ClienteResponseDTO getCurrentUserInfo(Jwt jwt);

    /**
     * Actualiza la información del usuario en la base de datos
     * @param jwt Token JWT de Auth0
     * @return ClienteResponseDTO actualizado
     */
    ClienteResponseDTO updateUserFromAuth0(Jwt jwt);

    /**
     * Sincroniza un usuario usando datos enviados directamente desde el frontend
     * @param userData Mapa con los datos del usuario (auth0Id, email, name, etc.)
     * @return ClienteResponseDTO con la información del cliente creado/actualizado
     */
    ClienteResponseDTO syncUserFromUserData(Map<String, Object> userData, Jwt jwt);
}