package com.elbuensabor.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "promocion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Promocion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_promocion")
    private Long idPromocion;

    @Column(nullable = false)
    private String denominacion;

    @Column(name = "fecha_desde", nullable = false)
    private LocalDateTime fechaDesde;

    @Column(name = "fecha_hasta", nullable = false)
    private LocalDateTime fechaHasta;

    @Column(name = "hora_desde", nullable = false)
    private LocalTime horaDesde;

    @Column(name = "hora_hasta", nullable = false)
    private LocalTime horaHasta;

    @Column(name = "descripcion_descuento")
    private String descripcionDescuento;

    @Column(name = "precio_promocional", nullable = false)
    private Double precioPromocional;

    @ManyToMany
    @JoinTable(
            name = "promocion_articulo",
            joinColumns = @JoinColumn(name = "id_promocion"),
            inverseJoinColumns = @JoinColumn(name = "id_articulo")
    )
    private List<Articulo> articulos = new ArrayList<>();

    @OneToMany(mappedBy = "promocion", cascade = CascadeType.ALL)
    private List<Imagen> imagenes = new ArrayList<>();

    @ManyToMany(mappedBy = "promociones")
    private List<SucursalEmpresa> sucursales = new ArrayList<>();
}
