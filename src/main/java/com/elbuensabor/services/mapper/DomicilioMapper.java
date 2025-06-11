package com.elbuensabor.services.mapper;

import com.elbuensabor.dto.request.DomicilioDTO;
import com.elbuensabor.dto.response.DomicilioResponseDTO;
import com.elbuensabor.entities.Domicilio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DomicilioMapper extends BaseMapper<Domicilio, DomicilioDTO> {

    // ==================== MAPEOS EXISTENTES ====================
    @Override
    @Mapping(target = "idDomicilio", ignore = true)
    @Mapping(target = "sucursalEmpresa", ignore = true)
    @Mapping(target = "pedidos", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    Domicilio toEntity(DomicilioDTO dto);

    @Override
    @Mapping(target = "idDomicilio", ignore = true)
    @Mapping(target = "sucursalEmpresa", ignore = true)
    @Mapping(target = "pedidos", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    void updateEntityFromDTO(DomicilioDTO dto, @MappingTarget Domicilio entity);

    // ==================== NUEVO MAPEO PARA PEDIDOS ====================
    @Mapping(target = "direccionCompleta", expression = "java(construirDireccionCompleta(entity))")
    DomicilioResponseDTO toResponseDTO(Domicilio entity);

    // ==================== MÉTODO AUXILIAR ====================
    default String construirDireccionCompleta(Domicilio domicilio) {
        if (domicilio == null) return "";

        return String.format("%s %d, %s (CP: %d)",
                domicilio.getCalle(),
                domicilio.getNumero(),
                domicilio.getLocalidad(),
                domicilio.getCp());
    }
}