package com.elbuensabor.services;

import com.elbuensabor.dto.response.ClienteResponseDTO;
import com.elbuensabor.entities.Rol;

import java.util.List;

public interface IRoleManagementService {

    /**
     * Cambiar el rol de un usuario
     */
    void changeUserRole(Long clienteId, Rol newRole);

    /**
     * Verificar si un usuario es administrador
     */
    boolean isAdmin(String auth0Id);

    /**
     * Obtener todos los usuarios con sus roles
     */
    List<ClienteResponseDTO> getAllUsersWithRoles();

    /**
     * Verificar si un usuario tiene un rol específico
     */
    boolean hasRole(String auth0Id, Rol role);
}