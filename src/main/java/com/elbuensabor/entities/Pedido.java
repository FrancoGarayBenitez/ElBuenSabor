package com.elbuensabor.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedido")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Long idPedido;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "hora_estimada_finalizacion")
    private LocalTime horaEstimadaFinalizacion;

    @Column(nullable = false)
    private Double total;

    @Column(name = "total_costo", nullable = false)
    private Double totalCosto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;

    private String observaciones;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_envio", nullable = false)
    private TipoEnvio tipoEnvio;

    @ManyToOne
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "id_domicilio")
    private Domicilio domicilio;

    @ManyToOne
    @JoinColumn(name = "id_sucursal", nullable = false)
    private SucursalEmpresa sucursal;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
    private List<DetallePedido> detalles = new ArrayList<>();

    @OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL)
    private Factura factura;
}
