package com.elbuensabor.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Long idPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_factura", nullable = false)
    private Factura factura;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pago", nullable = false)
    private FormaPago formaPago;

    @Column(name = "mercado_pago_preference_id")
    private String mercadoPagoPreferenceId;

    @Column(name = "mercado_pago_payment_id")
    private String mercadoPagoPaymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPago estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(nullable = false)
    private Double monto;

    @Column(nullable = false)
    private String moneda;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    // Relación con datos de Mercado Pago
    @OneToOne(mappedBy = "pago", cascade = CascadeType.ALL, orphanRemoval = true)
    private DatosMercadoPago datosMercadoPago;

    // Constructor para nuevo pago
    public Pago(Factura factura, Double monto, FormaPago formaPago) {
        this.factura = factura;
        this.monto = monto;
        this.formaPago = formaPago;
        this.estado = EstadoPago.PENDIENTE;
        this.moneda = "ARS";
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    // Método para verificar si es pago con Mercado Pago
    public boolean esMercadoPago() {
        return FormaPago.MERCADO_PAGO.equals(this.formaPago);
    }

    // Método para verificar si es pago en efectivo
    public boolean esEfectivo() {
        return FormaPago.EFECTIVO.equals(this.formaPago);
    }

    @Override
    public String toString() {
        return "Pago{" +
                "idPago=" + idPago +
                ", formaPago=" + formaPago +
                ", mercadoPagoPreferenceId='" + mercadoPagoPreferenceId + '\'' +
                ", estado='" + estado + '\'' +
                ", monto=" + monto +
                ", moneda='" + moneda + '\'' +
                ", fechaCreacion=" + fechaCreacion +
                ", facturaId=" + (factura != null ? factura.getIdFactura() : null) +
                '}';
    }
}

