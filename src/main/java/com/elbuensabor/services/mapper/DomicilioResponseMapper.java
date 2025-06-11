package com.elbuensabor.services.mapper;

import com.elbuensabor.dto.response.DomicilioResponseDTO;
import com.elbuensabor.entities.Domicilio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DomicilioResponseMapper {

    @Mapping(target = "direccionCompleta", expression = "java(construirDireccionCompleta(entity))")
    DomicilioResponseDTO toDTO(Domicilio entity);

    default String construirDireccionCompleta(Domicilio domicilio) {
        return String.format("%s %d, %s (CP: %d)",
                domicilio.getCalle(),
                domicilio.getNumero(),
                domicilio.getLocalidad(),
                domicilio.getCp());
    }
}
