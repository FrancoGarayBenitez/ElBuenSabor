package com.elbuensabor.services.mapper;

import com.elbuensabor.dto.request.DetallePedidoRequestDTO;
import com.elbuensabor.dto.response.DetallePedidoResponseDTO;
import com.elbuensabor.entities.DetallePedido;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DetallePedidoMapper {

    // ==================== ENTITY → RESPONSE DTO ====================
    @Mapping(source = "articulo.idArticulo", target = "idArticulo")
    @Mapping(source = "articulo.denominacion", target = "denominacionArticulo")
    @Mapping(source = "articulo.precioVenta", target = "precioUnitario")
    @Mapping(source = "articulo.unidadMedida.denominacion", target = "unidadMedida")
    @Mapping(target = "tiempoPreparacion", expression = "java(calcularTiempoPreparacion(entity.getArticulo()))")
    DetallePedidoResponseDTO toDTO(DetallePedido entity);

    // ==================== REQUEST DTO → ENTITY ====================
    @Mapping(target = "idDetallePedido", ignore = true)
    @Mapping(target = "subtotal", ignore = true) // Se calcula en el service
    @Mapping(target = "articulo", ignore = true) // Se asigna en el service
    @Mapping(target = "pedido", ignore = true) // Se asigna en el service
    DetallePedido toEntity(DetallePedidoRequestDTO dto);

    // ==================== MÉTODO AUXILIAR ====================
    default Integer calcularTiempoPreparacion(com.elbuensabor.entities.Articulo articulo) {
        if (articulo instanceof com.elbuensabor.entities.ArticuloManufacturado) {
            com.elbuensabor.entities.ArticuloManufacturado manufacturado =
                    (com.elbuensabor.entities.ArticuloManufacturado) articulo;
            return manufacturado.getTiempoEstimadoEnMinutos();
        }
        return 0; // Los insumos no tienen tiempo de preparación
    }
}