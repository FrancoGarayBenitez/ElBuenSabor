package com.elbuensabor.services.impl;

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

        if (pedido.getTipoEnvio() == TipoEnvio.DELIVERY) {
            gastosEnvio = 200.0;
            subtotal = subtotal - gastosEnvio;
        }

        Double descuento = 0.0;
        Double totalVenta = subtotal - descuento + gastosEnvio;

        factura.setSubTotal(subtotal);
        factura.setDescuento(descuento);
        factura.setGastosEnvio(gastosEnvio);
        factura.setTotalVenta(totalVenta);
    }

    // ✅ MAPEO ULTRA-SIMPLIFICADO SIN RIESGO DE RECURSIÓN
    private FacturaResponseDTO mapearFacturaSimple(Factura factura) {
        FacturaResponseDTO dto = new FacturaResponseDTO();

        // Mapeo básico
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

            if (pedido.getCliente() != null) {
                dto.setClienteId(pedido.getCliente().getIdCliente());
                dto.setNombreCliente(pedido.getCliente().getNombre());
                dto.setApellidoCliente(pedido.getCliente().getApellido());
            }
        }

        // ✅ CALCULAR PAGOS REALES desde la BD
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