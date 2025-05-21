package com.elbuensabor.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "factura")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Factura {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_factura")
    private Long idFactura;

    @Column(nullable = false)
    private LocalDate fechaFactura;

    @Column(name = "nro_comprobante", nullable = false, unique = true)
    private String nroComprobante;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pago", nullable = false)
    private FormaPago formaPago;

    @Column(nullable = false)
    private Double subTotal;

    @Column(nullable = false)
    private Double descuento;

    @Column(name = "gastos_envio", nullable = false)
    private Double gastosEnvio;

    @Column(name="total_venta", nullable = false)
    private Double totalVenta;

    @OneToOne(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    private DatosMercadoPago datosMercadoPago;

    @OneToOne
    @JoinColumn(name = "id_pedido")
    private Pedido pedido;
}
