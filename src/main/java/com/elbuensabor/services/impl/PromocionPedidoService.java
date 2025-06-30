package com.elbuensabor.services.impl;

import com.elbuensabor.dto.request.DetallePedidoRequestDTO;
import com.elbuensabor.dto.request.PedidoRequestDTO;
import com.elbuensabor.dto.request.PromocionAgrupadaDTO;
import com.elbuensabor.dto.response.PedidoResponseDTO;
import com.elbuensabor.entities.Articulo;
import com.elbuensabor.entities.DetallePedido;
import com.elbuensabor.entities.Promocion;
import com.elbuensabor.exceptions.ResourceNotFoundException;
import com.elbuensabor.repository.IArticuloRepository;
import com.elbuensabor.repository.IPromocionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PromocionPedidoService {

    private static final Logger logger = LoggerFactory.getLogger(PromocionPedidoService.class);

    @Autowired
    private IPromocionRepository promocionRepository;

    @Autowired
    private IArticuloRepository articuloRepository;

    // ==================== MÉTODO PRINCIPAL: APLICAR PROMOCIONES A PEDIDO ====================

    public PromocionesAplicadasDTO aplicarPromocionesAPedido(PedidoRequestDTO pedidoRequest) {
        logger.info("🎯 Aplicando promociones a pedido con {} detalles", pedidoRequest.getDetalles().size());

        PromocionesAplicadasDTO resultado = new PromocionesAplicadasDTO();
        resultado.setDetallesConPromociones(new ArrayList<>());
        resultado.setDescuentoTotal(0.0);
        resultado.setSubtotalOriginal(0.0);

        for (DetallePedidoRequestDTO detalleRequest : pedidoRequest.getDetalles()) {
            DetalleConPromocionDTO detalleConPromocion = procesarDetalleConPromocion(
                    detalleRequest,
                    pedidoRequest.getIdSucursal()
            );

            resultado.getDetallesConPromociones().add(detalleConPromocion);
            resultado.setSubtotalOriginal(resultado.getSubtotalOriginal() + detalleConPromocion.getSubtotalOriginal());
            resultado.setDescuentoTotal(resultado.getDescuentoTotal() + detalleConPromocion.getDescuentoAplicado());
        }

        resultado.setSubtotalFinal(resultado.getSubtotalOriginal() - resultado.getDescuentoTotal());
        resultado.generarResumen();

        logger.info("🎉 Promociones aplicadas: Subtotal original: ${}, Descuento: ${}, Final: ${}",
                resultado.getSubtotalOriginal(), resultado.getDescuentoTotal(), resultado.getSubtotalFinal());

        return resultado;
    }

    // ==================== PROCESAR DETALLE INDIVIDUAL ====================

    private DetalleConPromocionDTO procesarDetalleConPromocion(DetallePedidoRequestDTO detalleRequest, Long idSucursal) {
        DetalleConPromocionDTO detalle = new DetalleConPromocionDTO();

        // Obtener artículo
        Articulo articulo = articuloRepository.findById(detalleRequest.getIdArticulo())
                .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado: " + detalleRequest.getIdArticulo()));

        // Datos básicos
        detalle.setIdArticulo(articulo.getIdArticulo());
        detalle.setDenominacionArticulo(articulo.getDenominacion());
        detalle.setCantidad(detalleRequest.getCantidad());
        detalle.setPrecioUnitarioOriginal(articulo.getPrecioVenta());
        detalle.setSubtotalOriginal(articulo.getPrecioVenta() * detalleRequest.getCantidad());
        detalle.setObservaciones(detalleRequest.getObservaciones());

        // Aplicar promoción si fue seleccionada
        if (detalleRequest.getIdPromocionSeleccionada() != null) {
            aplicarPromocionADetalle(detalle, detalleRequest.getIdPromocionSeleccionada(), idSucursal);
        } else {
            // Sin promoción
            detalle.setDescuentoAplicado(0.0);
            detalle.setPrecioUnitarioFinal(articulo.getPrecioVenta());
            detalle.setSubtotalFinal(detalle.getSubtotalOriginal());
            detalle.setTienePromocion(false);
        }

        return detalle;
    }

    // ==================== APLICAR PROMOCIÓN A DETALLE ====================

    private void aplicarPromocionADetalle(DetalleConPromocionDTO detalle, Long idPromocion, Long idSucursal) {
        try {
            Optional<Promocion> promocionOpt = promocionRepository.findById(idPromocion);

            if (promocionOpt.isEmpty()) {
                logger.warn("⚠️ Promoción {} no encontrada, se omite", idPromocion);
                sinPromocion(detalle);
                return;
            }

            Promocion promocion = promocionOpt.get();

            // Validar que la promoción esté vigente
            if (!promocion.estaVigente()) {
                logger.warn("⚠️ Promoción '{}' no está vigente, se omite", promocion.getDenominacion());
                sinPromocion(detalle);
                return;
            }

            // Validar que aplique para el artículo
            if (!promocion.aplicaParaArticulo(detalle.getIdArticulo())) {
                logger.warn("⚠️ Promoción '{}' no aplica para artículo {}, se omite",
                        promocion.getDenominacion(), detalle.getIdArticulo());
                sinPromocion(detalle);
                return;
            }

            // Validar que aplique para la sucursal
            if (!promocion.aplicaParaSucursal(idSucursal)) {
                logger.warn("⚠️ Promoción '{}' no aplica para sucursal {}, se omite",
                        promocion.getDenominacion(), idSucursal);
                sinPromocion(detalle);
                return;
            }

            // Validar cantidad mínima
            if (detalle.getCantidad() < promocion.getCantidadMinima()) {
                logger.warn("⚠️ Promoción '{}' requiere cantidad mínima {}, actual: {}, se omite",
                        promocion.getDenominacion(), promocion.getCantidadMinima(), detalle.getCantidad());
                sinPromocion(detalle);
                return;
            }

            // ✅ APLICAR PROMOCIÓN
            Double descuento = promocion.calcularDescuento(detalle.getPrecioUnitarioOriginal(), detalle.getCantidad());

            detalle.setDescuentoAplicado(descuento);
            detalle.setPrecioUnitarioFinal(detalle.getPrecioUnitarioOriginal() - (descuento / detalle.getCantidad()));
            detalle.setSubtotalFinal(detalle.getSubtotalOriginal() - descuento);
            detalle.setTienePromocion(true);

            // Información de la promoción
            detalle.setPromocionAplicada(new DetalleConPromocionDTO.PromocionInfoDTO());
            detalle.getPromocionAplicada().setIdPromocion(promocion.getIdPromocion());
            detalle.getPromocionAplicada().setDenominacion(promocion.getDenominacion());
            detalle.getPromocionAplicada().setDescripcion(promocion.getDescripcionDescuento());
            detalle.getPromocionAplicada().setTipoDescuento(promocion.getTipoDescuento().toString());
            detalle.getPromocionAplicada().setValorDescuento(promocion.getValorDescuento());
            detalle.getPromocionAplicada().setResumenDescuento(
                    String.format("%s - Ahorro: $%.2f", promocion.getDenominacion(), descuento)
            );

            logger.info("✅ Promoción '{}' aplicada a {}: descuento ${}",
                    promocion.getDenominacion(), detalle.getDenominacionArticulo(), descuento);

        } catch (Exception e) {
            logger.error("❌ Error aplicando promoción {}: {}", idPromocion, e.getMessage());
            sinPromocion(detalle);
        }
    }

    private void sinPromocion(DetalleConPromocionDTO detalle) {
        detalle.setDescuentoAplicado(0.0);
        detalle.setPrecioUnitarioFinal(detalle.getPrecioUnitarioOriginal());
        detalle.setSubtotalFinal(detalle.getSubtotalOriginal());
        detalle.setTienePromocion(false);
        detalle.setPromocionAplicada(null);
    }

    // ==================== DTOs AUXILIARES ====================

    public static class PromocionesAplicadasDTO {
        private List<DetalleConPromocionDTO> detallesConPromociones;
        private Double subtotalOriginal;
        private Double descuentoTotal;
        private Double subtotalFinal;
        private String resumenPromociones;

        public void generarResumen() {
            long promocionesAplicadas = detallesConPromociones.stream()
                    .mapToLong(d -> d.getTienePromocion() ? 1 : 0)
                    .sum();

            if (promocionesAplicadas == 0) {
                resumenPromociones = "Sin promociones aplicadas";
            } else {
                resumenPromociones = String.format("%d promoción(es) aplicada(s) - Ahorro total: $%.2f",
                        promocionesAplicadas, descuentoTotal);
            }
        }

        // Getters y setters
        public List<DetalleConPromocionDTO> getDetallesConPromociones() { return detallesConPromociones; }
        public void setDetallesConPromociones(List<DetalleConPromocionDTO> detallesConPromociones) { this.detallesConPromociones = detallesConPromociones; }
        public Double getSubtotalOriginal() { return subtotalOriginal; }
        public void setSubtotalOriginal(Double subtotalOriginal) { this.subtotalOriginal = subtotalOriginal; }
        public Double getDescuentoTotal() { return descuentoTotal; }
        public void setDescuentoTotal(Double descuentoTotal) { this.descuentoTotal = descuentoTotal; }
        public Double getSubtotalFinal() { return subtotalFinal; }
        public void setSubtotalFinal(Double subtotalFinal) { this.subtotalFinal = subtotalFinal; }
        public String getResumenPromociones() { return resumenPromociones; }
        public void setResumenPromociones(String resumenPromociones) { this.resumenPromociones = resumenPromociones; }
    }

    public static class DetalleConPromocionDTO {
        private Long idArticulo;
        private String denominacionArticulo;
        private Integer cantidad;
        private Double precioUnitarioOriginal;
        private Double precioUnitarioFinal;
        private Double subtotalOriginal;
        private Double subtotalFinal;
        private Double descuentoAplicado;
        private Boolean tienePromocion;
        private String observaciones;
        private PromocionInfoDTO promocionAplicada;

        public static class PromocionInfoDTO {
            private Long idPromocion;
            private String denominacion;
            private String descripcion;
            private String tipoDescuento;
            private Double valorDescuento;
            private String resumenDescuento;

            // Getters y setters
            public Long getIdPromocion() { return idPromocion; }
            public void setIdPromocion(Long idPromocion) { this.idPromocion = idPromocion; }
            public String getDenominacion() { return denominacion; }
            public void setDenominacion(String denominacion) { this.denominacion = denominacion; }
            public String getDescripcion() { return descripcion; }
            public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
            public String getTipoDescuento() { return tipoDescuento; }
            public void setTipoDescuento(String tipoDescuento) { this.tipoDescuento = tipoDescuento; }
            public Double getValorDescuento() { return valorDescuento; }
            public void setValorDescuento(Double valorDescuento) { this.valorDescuento = valorDescuento; }
            public String getResumenDescuento() { return resumenDescuento; }
            public void setResumenDescuento(String resumenDescuento) { this.resumenDescuento = resumenDescuento; }
        }

        // Getters y setters completos
        public Long getIdArticulo() { return idArticulo; }
        public void setIdArticulo(Long idArticulo) { this.idArticulo = idArticulo; }
        public String getDenominacionArticulo() { return denominacionArticulo; }
        public void setDenominacionArticulo(String denominacionArticulo) { this.denominacionArticulo = denominacionArticulo; }
        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
        public Double getPrecioUnitarioOriginal() { return precioUnitarioOriginal; }
        public void setPrecioUnitarioOriginal(Double precioUnitarioOriginal) { this.precioUnitarioOriginal = precioUnitarioOriginal; }
        public Double getPrecioUnitarioFinal() { return precioUnitarioFinal; }
        public void setPrecioUnitarioFinal(Double precioUnitarioFinal) { this.precioUnitarioFinal = precioUnitarioFinal; }
        public Double getSubtotalOriginal() { return subtotalOriginal; }
        public void setSubtotalOriginal(Double subtotalOriginal) { this.subtotalOriginal = subtotalOriginal; }
        public Double getSubtotalFinal() { return subtotalFinal; }
        public void setSubtotalFinal(Double subtotalFinal) { this.subtotalFinal = subtotalFinal; }
        public Double getDescuentoAplicado() { return descuentoAplicado; }
        public void setDescuentoAplicado(Double descuentoAplicado) { this.descuentoAplicado = descuentoAplicado; }
        public Boolean getTienePromocion() { return tienePromocion; }
        public void setTienePromocion(Boolean tienePromocion) { this.tienePromocion = tienePromocion; }
        public String getObservaciones() { return observaciones; }
        public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
        public PromocionInfoDTO getPromocionAplicada() { return promocionAplicada; }
        public void setPromocionAplicada(PromocionInfoDTO promocionAplicada) { this.promocionAplicada = promocionAplicada; }
    }

    public PromocionesAplicadasDTO aplicarPromocionesAPedidoConAgrupada(PedidoRequestDTO pedidoRequest) {
        System.out.println("🎁 Procesando promociones con promoción agrupada...");

        // Aplicar promociones individuales normalmente
        PromocionesAplicadasDTO promocionesIndividuales = aplicarPromocionesAPedido(pedidoRequest);

        // Si hay promoción agrupada, agregar su información
        if (pedidoRequest.getPromocionAgrupada() != null) {
            PromocionAgrupadaDTO promocionAgrupada = pedidoRequest.getPromocionAgrupada();

            // Modificar el resumen para incluir la promoción agrupada
            String resumenOriginal = promocionesIndividuales.getResumenPromociones();
            String resumenConAgrupada = promocionAgrupada.getDenominacion() +
                    " (" + promocionAgrupada.getValorDescuento() + "% OFF)" +
                    (resumenOriginal.isEmpty() ? "" : " + " + resumenOriginal);

            promocionesIndividuales.setResumenPromociones(resumenConAgrupada);

            // Agregar el descuento de la promoción agrupada al total
            double descuentoAdicional = promocionAgrupada.getDescuentoAplicado();
            promocionesIndividuales.setDescuentoTotal(
                    promocionesIndividuales.getDescuentoTotal() + descuentoAdicional
            );
            promocionesIndividuales.setSubtotalFinal(
                    promocionesIndividuales.getSubtotalFinal() - descuentoAdicional
            );

            System.out.println("🎁 Promoción agrupada procesada: " + promocionAgrupada.getDenominacion());
            System.out.println("🎁 Descuento adicional: $" + descuentoAdicional);
        }

        return promocionesIndividuales;
    }
}