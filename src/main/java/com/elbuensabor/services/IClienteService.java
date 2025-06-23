package com.elbuensabor.services;

import com.elbuensabor.dto.response.ClienteResponseDTO;
import com.elbuensabor.entities.Cliente;

/**
 * Servicio para manejo de clientes
 * Extends IGenericService para operaciones CRUD básicas
 */
public interface IClienteService extends IGenericService<Cliente, Long, ClienteResponseDTO> {

    /**
     * Busca un cliente por su Auth0 ID
     * Utilizado para operaciones de usuarios autenticados con Auth0
     *
     * @param auth0Id ID del usuario en Auth0
     * @return ClienteResponseDTO del cliente encontrado
     * @throws ResourceNotFoundException si no se encuentra el cliente
     */
    ClienteResponseDTO findByAuth0Id(String auth0Id);

    /**
     * Verifica si existe un cliente con el Auth0 ID dado
     *
     * @param auth0Id ID del usuario en Auth0
     * @return true si existe, false si no
     */
    boolean existsByAuth0Id(String auth0Id);

    /**
     * Busca un cliente por email
     * Útil para migraciones y validaciones
     *
     * @param email Email del cliente
     * @return ClienteResponseDTO del cliente encontrado
     * @throws ResourceNotFoundException si no se encuentra el cliente
     */
    ClienteResponseDTO findByEmail(String email);
}