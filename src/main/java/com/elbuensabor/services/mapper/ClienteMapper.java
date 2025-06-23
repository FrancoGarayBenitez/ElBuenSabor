package com.elbuensabor.services.mapper;

import com.elbuensabor.dto.request.ClienteRegisterDTO;
import com.elbuensabor.dto.response.ClienteResponseDTO;
import com.elbuensabor.entities.Cliente;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {DomicilioMapper.class})
public interface ClienteMapper extends BaseMapper<Cliente, ClienteResponseDTO> {

    // ENTITY → RESPONSE DTO
    @Override
    @Mapping(source = "usuario.email", target = "email")
    @Mapping(source = "usuario.rol", target = "rol")
    @Mapping(target = "domicilios", ignore = true)
    ClienteResponseDTO toDTO(Cliente entity);

    // RESPONSE DTO → ENTITY (para el método genérico)
    @Override
    @Mapping(target = "idCliente", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "pedidos", ignore = true)
    @Mapping(target = "imagen", ignore = true)
    Cliente toEntity(ClienteResponseDTO dto);

    // REGISTER DTO → ENTITY
    @Mapping(target = "idCliente", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "pedidos", ignore = true)
    @Mapping(target = "imagen", ignore = true)
    @Mapping(target = "domicilios", ignore = true) // Se maneja manualmente en el service
    Cliente toEntity(ClienteRegisterDTO registerDTO);

    // UPDATE desde RESPONSE DTO
    @Override
    @Mapping(target = "idCliente", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "pedidos", ignore = true)
    @Mapping(target = "imagen", ignore = true)
    void updateEntityFromDTO(ClienteResponseDTO dto, @MappingTarget Cliente entity);
}
