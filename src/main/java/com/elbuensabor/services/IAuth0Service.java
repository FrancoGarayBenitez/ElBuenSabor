package com.elbuensabor.services;

import com.elbuensabor.dto.request.ClienteRegisterDTO;
import com.elbuensabor.dto.response.ClienteResponseDTO;
import com.elbuensabor.dto.response.LoginResponseDTO;
import com.elbuensabor.entities.Cliente;
import com.elbuensabor.entities.Rol;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;

/**
 * Servicio para manejo de usuarios autenticados con Auth0
 * Maneja la sincronización entre Auth0 y la base de datos local
 */
public interface IAuth0Service {

    /**
     * Procesa un usuario autenticado con Auth0 y retorna la respuesta de login
     * Sincroniza o crea el usuario en la base de datos local
     */
    LoginResponseDTO processAuth0User(Jwt jwt, Map<String, Object> userData);

    /**
     * Registra un nuevo cliente con datos adicionales desde Auth0
     * El usuario ya debe estar autenticado en Auth0
     */
    ClienteResponseDTO registerClienteFromAuth0(Jwt jwt, ClienteRegisterDTO registerDTO);

    /**
     * Completa el perfil de un usuario ya existente con datos adicionales
     */
    ClienteResponseDTO completeUserProfile(Jwt jwt, ClienteRegisterDTO profileData);

    /**
     * Obtiene el perfil del usuario actual desde el JWT
     */
    Map<String, Object> getCurrentUserProfile(Jwt jwt);

    /**
     * Actualiza los roles del usuario basado en el JWT actual
     */
    Map<String, Object> refreshUserRoles(Jwt jwt);

    /**
     * Busca o crea un cliente basado en datos de Auth0
     * Método interno para sincronización de usuarios
     */
    Cliente findOrCreateClienteFromAuth0(String auth0Id, String email, String nombre, String apellido, Rol rol);
}