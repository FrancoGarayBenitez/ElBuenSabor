package com.elbuensabor.services;

import com.elbuensabor.dto.response.UsuarioGridResponseDTO;

import java.util.List;

/**
 * Interfaz para servicios de gestión de usuarios
 */
public interface IUsuarioService {

    /**
     * Obtiene todos los usuarios para mostrar en la grilla administrativa
     */
    List<UsuarioGridResponseDTO> obtenerUsuariosParaGrilla();

    /**
     * Obtiene un usuario específico por ID
     */
    UsuarioGridResponseDTO obtenerUsuarioPorId(Long idUsuario);

    /**
     * Cambia el rol de un usuario
     */
    UsuarioGridResponseDTO cambiarRol(Long idUsuario, String nuevoRol);

    /**
     * Cambia el estado activo/inactivo de un usuario
     */
    UsuarioGridResponseDTO cambiarEstado(Long idUsuario, boolean activo);

    /**
     * Obtiene el ID de usuario por Auth0 ID
     */
    Long obtenerIdUsuarioPorAuth0Id(String auth0Id);

    /**
     * Cuenta la cantidad de administradores activos en el sistema
     */
    long contarAdministradoresActivos();
}