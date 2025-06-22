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
     *
     * @param jwt Token JWT de Auth0
     * @param userData Datos adicionales del usuario desde el frontend (opcional)
     * @return LoginResponseDTO con información del usuario
     */
    LoginResponseDTO processAuth0User(Jwt jwt, Map<String, Object> userData);

    /**
     * Registra un nuevo cliente con datos adicionales desde Auth0
     * El usuario ya debe estar autenticado en Auth0
     *
     * @param jwt Token JWT de Auth0 del usuario
     * @param registerDTO Datos adicionales del cliente a registrar
     * @return ClienteResponseDTO con información del cliente registrado
     */
    ClienteResponseDTO registerClienteFromAuth0(Jwt jwt, ClienteRegisterDTO registerDTO);

    /**
     * Busca o crea un cliente basado en datos de Auth0
     * Método interno para sincronización de usuarios
     *
     * @param auth0Id ID del usuario en Auth0
     * @param email Email del usuario
     * @param nombre Nombre del usuario
     * @param apellido Apellido del usuario
     * @param rol Rol del usuario
     * @return Cliente encontrado o creado
     */
    Cliente findOrCreateClienteFromAuth0(String auth0Id, String email, String nombre, String apellido, Rol rol);
}