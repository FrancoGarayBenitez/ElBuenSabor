package com.elbuensabor.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="articulo_manufacturado")
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticuloManufacturado extends Articulo{

    @Column(length = 1000)
    private String descripcion;

    @Column(name = "tiempo_estimado")
    private Integer tiempoEstimadoEnMinutos;

    @Column
    private String preparacion;

    @OneToMany(mappedBy = "articuloManufacturado", cascade = CascadeType.ALL)
    private List<ArticuloManufacturadoDetalle> detalles = new ArrayList<>();
}
