package com.elbuensabor.services.mapper;

import com.elbuensabor.dto.request.ClienteRegisterDTO;
import com.elbuensabor.dto.response.ClienteResponseDTO;
import com.elbuensabor.entities.Cliente;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper para conversiones entre Cliente entity y DTOs
 * Adaptado para trabajar con Auth0 (sin passwords locales)
 */
@Mapper(componentModel = "spring", uses = {DomicilioMapper.class})
public interface ClienteMapper extends BaseMapper<Cliente, ClienteResponseDTO> {

    /**
     * Convierte entidad Cliente a DTO de respuesta
     * Incluye el email del usuario asociado
     */
    @Override
    @Mapping(source = "usuario.email", target = "email")
    ClienteResponseDTO toDTO(Cliente entity);

    /**
     * Convierte DTO de respuesta a entidad Cliente
     * Usado en operaciones de actualización genéricas
     * Ignora campos sensibles que se manejan por separado
     */
    @Override
    @Mapping(target = "idCliente", ignore = true)
    @Mapping(target = "usuario", ignore = true)  // Usuario/Auth0 se maneja por separado
    @Mapping(target = "pedidos", ignore = true)  // Relación se maneja por separado
    @Mapping(target = "imagen", ignore = true)   // Se maneja manualmente si está presente
    Cliente toEntity(ClienteResponseDTO dto);

    /**
     * Convierte DTO de registro a entidad Cliente
     * Usado para crear nuevos clientes desde Auth0
     * El usuario ya debe estar autenticado en Auth0
     */
    @Mapping(target = "idCliente", ignore = true)
    @Mapping(target = "usuario", ignore = true)      // Se crea manualmente en el servicio
    @Mapping(target = "pedidos", ignore = true)      // Lista vacía inicial
    @Mapping(target = "imagen", ignore = true)       // Se maneja manualmente en el servicio
    @Mapping(target = "domicilios", ignore = true)   // Se maneja manualmente en el servicio
    Cliente toEntity(ClienteRegisterDTO registerDTO);

    /**
     * Actualiza una entidad Cliente existente con datos del DTO
     * Preserva campos críticos como ID, usuario y relaciones
     * Usado en operaciones PUT
     */
    @Override
    @Mapping(target = "idCliente", ignore = true)    // ID no se puede cambiar
    @Mapping(target = "usuario", ignore = true)      // Usuario/Auth0 se actualiza por separado
    @Mapping(target = "pedidos", ignore = true)      // Historial de pedidos se mantiene
    @Mapping(target = "imagen", ignore = true)       // Imagen se maneja por separado
    void updateEntityFromDTO(ClienteResponseDTO dto, @MappingTarget Cliente entity);
}
