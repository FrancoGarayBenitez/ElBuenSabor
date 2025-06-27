package com.elbuensabor.services.impl;

import com.elbuensabor.dto.response.DetallePedidoResponseDTO;
import com.elbuensabor.dto.response.DomicilioResponseDTO;
import com.elbuensabor.dto.response.FacturaResponseDTO;
import com.elbuensabor.dto.response.PagoSummaryDTO;
import com.elbuensabor.entities.*;
import com.elbuensabor.exceptions.DuplicateResourceException;
import com.elbuensabor.exceptions.ResourceNotFoundException;
import com.elbuensabor.repository.IFacturaRepository;
import com.elbuensabor.repository.IPagoRepository;
import com.elbuensabor.services.IFacturaService;
import com.elbuensabor.services.mapper.FacturaMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FacturaServiceImpl extends GenericServiceImpl<Factura, Long, FacturaResponseDTO, IFacturaRepository, FacturaMapper>
        implements IFacturaService {
    private static final Logger logger = LoggerFactory.getLogger(FacturaServiceImpl.class);

    @Autowired
    private IPagoRepository pagoRepository;

    @Autowired
    public FacturaServiceImpl(IFacturaRepository repository, FacturaMapper mapper) {
        super(repository, mapper, Factura.class, FacturaResponseDTO.class);
    }

    @Override
    @Transactional
    public FacturaResponseDTO crearFacturaFromPedido(Pedido pedido) {
        // Validar que el pedido no tenga ya una factura
        if (repository.existsByPedidoIdPedido(pedido.getIdPedido())) {
            throw new DuplicateResourceException("El pedido con ID " + pedido.getIdPedido() + " ya tiene una factura asociada");
        }

        // Crear nueva factura
        Factura factura = new Factura();
        factura.setPedido(pedido);
        factura.setFechaFactura(LocalDate.now());
        factura.setNroComprobante(generarNumeroComprobante());

        // Calcular totales
        calcularTotalesFactura(factura, pedido);

        Factura facturaGuardada = repository.save(factura);
        return mapearFacturaSimple(facturaGuardada);
    }

    @Override
    @Transactional(readOnly = true)
    public FacturaResponseDTO findByPedidoId(Long pedidoId) {
        Factura factura = repository.findByPedidoIdPedido(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró factura para el pedido con ID: " + pedidoId));
        return mapearFacturaSimple(factura);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacturaResponseDTO> findByClienteId(Long clienteId) {
        List<Factura> facturas = repository.findByClienteId(clienteId);
        return facturas.stream()
                .map(this::mapearFacturaSimple)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacturaResponseDTO> findByFechaRange(LocalDate fechaInicio, LocalDate fechaFin) {
        List<Factura> facturas = repository.findByFechaFacturaBetween(fechaInicio, fechaFin);
        return facturas.stream()
                .map(this::mapearFacturaSimple)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacturaResponseDTO> findFacturasPendientesPago() {
        List<Factura> facturas = repository.findFacturasPendientesPago();
        return facturas.stream()
                .map(this::mapearFacturaSimple)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeFacturaParaPedido(Long pedidoId) {
        return repository.existsByPedidoIdPedido(pedidoId);
    }

    @Override
    public String generarNumeroComprobante() {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefijo = "FAC-" + fecha + "-";

        // Generar número secuencial simple
        long timestamp = System.currentTimeMillis() % 10000; // Últimos 4 dígitos del timestamp
        return prefijo + String.format("%04d", timestamp);
    }

    @Override
    @Transactional(readOnly = true)
    public FacturaResponseDTO calcularTotales(Pedido pedido) {
        Factura facturaTemp = new Factura();
        facturaTemp.setPedido(pedido);
        calcularTotalesFactura(facturaTemp, pedido);
        return mapearFacturaSimple(facturaTemp);
    }

    @Override
    @Transactional(readOnly = true)
    public FacturaResponseDTO findById(Long id) {
        Factura factura = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factura con ID " + id + " no encontrada"));
        return mapearFacturaSimple(factura);
    }

    // ==================== MÉTODOS AUXILIARES PRIVADOS ====================

    private void calcularTotalesFactura(Factura factura, Pedido pedido) {
        Double subtotal = pedido.getTotal();
        Double gastosEnvio = 0.0;
        Double descuento = 0.0;

        // 🚚 DELIVERY: Agregar gastos de envío
        if (pedido.getTipoEnvio() == TipoEnvio.DELIVERY) {
            gastosEnvio = 200.0;
            // Para DELIVERY: el total del pedido ya incluye gastos, los separamos
            subtotal = subtotal - gastosEnvio;
        }

        // 🏪 TAKE_AWAY: Aplicar descuento del 10%
        else if (pedido.getTipoEnvio() == TipoEnvio.TAKE_AWAY) {
            descuento = subtotal * 0.10; // 10% de descuento
            logger.info("✅ Descuento TAKE_AWAY aplicado: {}% sobre ${} = ${}",
                    10, subtotal, descuento);
        }

        // Calcular total final
        Double totalVenta = subtotal - descuento + gastosEnvio;

        // Asignar valores a la factura
        factura.setSubTotal(subtotal);
        factura.setDescuento(descuento);
        factura.setGastosEnvio(gastosEnvio);
        factura.setTotalVenta(totalVenta);

        logger.info("💰 TOTALES CALCULADOS:");
        logger.info("   Subtotal: ${}", subtotal);
        logger.info("   Descuento: ${}", descuento);
        logger.info("   Gastos Envío: ${}", gastosEnvio);
        logger.info("   TOTAL: ${}", totalVenta);
    }

    // ✅ MÉTODO ACTUALIZADO para mapear datos completos del pedido
    private FacturaResponseDTO mapearFacturaSimple(Factura factura) {
        FacturaResponseDTO dto = new FacturaResponseDTO();

        // Mapeo básico de factura
        dto.setIdFactura(factura.getIdFactura());
        dto.setFechaFactura(factura.getFechaFactura());
        dto.setNroComprobante(factura.getNroComprobante());
        dto.setSubTotal(factura.getSubTotal());
        dto.setDescuento(factura.getDescuento());
        dto.setGastosEnvio(factura.getGastosEnvio());
        dto.setTotalVenta(factura.getTotalVenta());

        // Información del pedido
        if (factura.getPedido() != null) {
            Pedido pedido = factura.getPedido();
            dto.setPedidoId(pedido.getIdPedido());
            dto.setEstadoPedido(pedido.getEstado() != null ? pedido.getEstado().toString() : null);
            dto.setTipoEnvio(pedido.getTipoEnvio() != null ? pedido.getTipoEnvio().toString() : null);
            dto.setObservacionesPedido(pedido.getObservaciones());

            // 🆕 INFORMACIÓN COMPLETA DEL CLIENTE
            if (pedido.getCliente() != null) {
                Cliente cliente = pedido.getCliente();
                dto.setClienteId(cliente.getIdCliente());
                dto.setNombreCliente(cliente.getNombre());
                dto.setApellidoCliente(cliente.getApellido());
                dto.setTelefonoCliente(cliente.getTelefono());
            }

            // 🆕 DOMICILIO DE ENTREGA (si es DELIVERY)
            if (pedido.getDomicilio() != null && "DELIVERY".equals(dto.getTipoEnvio())) {
                Domicilio domicilio = pedido.getDomicilio();
                DomicilioResponseDTO domicilioDTO = new DomicilioResponseDTO();
                domicilioDTO.setIdDomicilio(domicilio.getIdDomicilio());
                domicilioDTO.setCalle(domicilio.getCalle());
                domicilioDTO.setNumero(domicilio.getNumero());
                domicilioDTO.setLocalidad(domicilio.getLocalidad());
                domicilioDTO.setCp(domicilio.getCp());
                dto.setDomicilioEntrega(domicilioDTO);
            }

            // 🆕 DETALLES REALES DEL PEDIDO
            if (pedido.getDetalles() != null && !pedido.getDetalles().isEmpty()) {
                logger.info("🔍 DEBUG: Cargando {} detalles reales del pedido {}",
                        pedido.getDetalles().size(), pedido.getIdPedido());

                List<DetallePedidoResponseDTO> detallesDTO = new ArrayList<>();

                for (DetallePedido detalle : pedido.getDetalles()) {
                    logger.info("🔍 DEBUG: Detalle - {} x{} = ${}",
                            detalle.getArticulo().getDenominacion(),
                            detalle.getCantidad(),
                            detalle.getSubtotal());
                    DetallePedidoResponseDTO detalleDTO = new DetallePedidoResponseDTO();
                    detalleDTO.setIdDetallePedido(detalle.getIdDetallePedido());
                    detalleDTO.setCantidad(detalle.getCantidad());
                    detalleDTO.setSubtotal(detalle.getSubtotal());
                    detalleDTO.setObservaciones(detalle.getObservaciones());

                    // Información del artículo
                    if (detalle.getArticulo() != null) {
                        Articulo articulo = detalle.getArticulo();
                        detalleDTO.setIdArticulo(articulo.getIdArticulo());
                        detalleDTO.setDenominacionArticulo(articulo.getDenominacion());

                        // Calcular precio unitario desde subtotal y cantidad
                        if (detalle.getCantidad() != null && detalle.getCantidad() > 0) {
                            double precioUnitario = detalle.getSubtotal() / detalle.getCantidad();
                            detalleDTO.setPrecioUnitario(precioUnitario);
                        }

                        // Otros campos del artículo si están disponibles
                        if (articulo.getUnidadMedida() != null) {
                            detalleDTO.setUnidadMedida(articulo.getUnidadMedida().getDenominacion());
                        }
                    }

                    detallesDTO.add(detalleDTO);
                }

                dto.setDetallesPedido(detallesDTO);
            }
        }

        // ✅ CALCULAR PAGOS REALES desde la BD (código existente sin cambios)
        try {
            List<Pago> pagosFactura = pagoRepository.findByFacturaIdFactura(factura.getIdFactura());

            List<PagoSummaryDTO> pagosSummary = new ArrayList<>();
            double totalPagado = 0.0;

            for (Pago pago : pagosFactura) {
                if (EstadoPago.APROBADO.equals(pago.getEstado())) {
                    totalPagado += pago.getMonto();

                    PagoSummaryDTO summary = new PagoSummaryDTO();
                    summary.setIdPago(pago.getIdPago());
                    summary.setFormaPago(pago.getFormaPago().name());
                    summary.setEstado(pago.getEstado().name());
                    summary.setMonto(pago.getMonto());
                    summary.setFechaCreacion(pago.getFechaCreacion().format(
                            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

                    pagosSummary.add(summary);
                }
            }

            dto.setPagos(pagosSummary);
            dto.setTotalPagado(totalPagado);
            dto.setSaldoPendiente(factura.getTotalVenta() - totalPagado);
            dto.setCompletamentePagada(totalPagado >= factura.getTotalVenta());

            logger.info("Factura {}: {} pagos encontrados, total pagado: ${}",
                    factura.getIdFactura(), pagosFactura.size(), totalPagado);

        } catch (Exception e) {
            logger.error("Error calculando pagos: {}", e.getMessage());
            // Fallback a valores por defecto
            dto.setPagos(new ArrayList<>());
            dto.setTotalPagado(0.0);
            dto.setSaldoPendiente(factura.getTotalVenta());
            dto.setCompletamentePagada(false);
        }

        return dto;
    }
}