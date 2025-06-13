package com.elbuensabor.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    // REMOVIDO: FormaPago - ahora está en cada Pago individual

    @Column(nullable = false)
    private Double subTotal;

    @Column(nullable = false)
    private Double descuento;

    @Column(name = "gastos_envio", nullable = false)
    private Double gastosEnvio;

    @Column(name="total_venta", nullable = false)
    private Double totalVenta;

    // REMOVIDO: DatosMercadoPago - ahora está en cada Pago individual

    @OneToOne
    @JoinColumn(name = "id_pedido")
    private Pedido pedido;

    // Nueva relación con pagos
    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Pago> pagos = new ArrayList<>();

    // Métodos de conveniencia
    public void addPago(Pago pago) {
        pagos.add(pago);
        pago.setFactura(this);
    }

    public void removePago(Pago pago) {
        pagos.remove(pago);
        pago.setFactura(null);
    }

    // Método útil para obtener el total pagado
    public Double getTotalPagado() {
        return pagos.stream()
                .filter(pago -> pago.getEstado() == EstadoPago.APROBADO)
                .mapToDouble(Pago::getMonto)
                .sum();
    }

    // Método para verificar si está completamente pagada
    public boolean isCompletamentePagada() {
        return getTotalPagado() >= totalVenta;
    }

    // Método para obtener el saldo pendiente
    public Double getSaldoPendiente() {
        return totalVenta - getTotalPagado();
    }
}