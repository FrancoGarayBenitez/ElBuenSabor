package com.elbuensabor.services;

import com.elbuensabor.dto.request.DomicilioRequestDTO;
import com.elbuensabor.dto.response.DomicilioResponseDTO;

import java.util.List;

/**
 * Servicio para gestión de domicilios desde el perfil del usuario
 * Todas las operaciones están restringidas al usuario autenticado
 */
public interface IDomicilioPerfilService {

    /**
     * Obtiene todos los domicilios del usuario autenticado
     * Ordenados por principal primero
     *
     * @param auth0Id ID del usuario en Auth0
     * @return Lista de domicilios del usuario
     */
    List<DomicilioResponseDTO> getMisDomicilios(String auth0Id);

    /**
     * Obtiene el domicilio principal del usuario autenticado
     *
     * @param auth0Id ID del usuario en Auth0
     * @return Domicilio principal o null si no tiene
     */
    DomicilioResponseDTO getMiDomicilioPrincipal(String auth0Id);

    /**
     * Crea un nuevo domicilio para el usuario autenticado
     * Si es marcado como principal, actualiza los demás automáticamente
     *
     * @param auth0Id ID del usuario en Auth0
     * @param domicilioDTO Datos del nuevo domicilio
     * @return Domicilio creado
     */
    DomicilioResponseDTO crearMiDomicilio(String auth0Id, DomicilioRequestDTO domicilioDTO);

    /**
     * Actualiza un domicilio del usuario autenticado
     * Valida que el domicilio pertenezca al usuario
     * Si es marcado como principal, actualiza los demás automáticamente
     *
     * @param auth0Id ID del usuario en Auth0
     * @param domicilioId ID del domicilio a actualizar
     * @param domicilioDTO Nuevos datos del domicilio
     * @return Domicilio actualizado
     */
    DomicilioResponseDTO actualizarMiDomicilio(String auth0Id, Long domicilioId, DomicilioRequestDTO domicilioDTO);

    /**
     * Elimina un domicilio del usuario autenticado
     * Valida que el domicilio pertenezca al usuario
     * Si era el principal, no marca automáticamente otro como principal
     *
     * @param auth0Id ID del usuario en Auth0
     * @param domicilioId ID del domicilio a eliminar
     */
    void eliminarMiDomicilio(String auth0Id, Long domicilioId);

    /**
     * Marca un domicilio específico como principal
     * Quita el estado principal de los demás automáticamente
     *
     * @param auth0Id ID del usuario en Auth0
     * @param domicilioId ID del domicilio a marcar como principal
     * @return Domicilio marcado como principal
     */
    DomicilioResponseDTO marcarComoPrincipal(String auth0Id, Long domicilioId);

    /**
     * Obtiene un domicilio específico del usuario autenticado
     * Valida que el domicilio pertenezca al usuario
     *
     * @param auth0Id ID del usuario en Auth0
     * @param domicilioId ID del domicilio a buscar
     * @return Domicilio encontrado
     */
    DomicilioResponseDTO getMiDomicilio(String auth0Id, Long domicilioId);

    /**
     * Cuenta cuántos domicilios tiene el usuario
     *
     * @param auth0Id ID del usuario en Auth0
     * @return Cantidad de domicilios
     */
    long contarMisDomicilios(String auth0Id);
}