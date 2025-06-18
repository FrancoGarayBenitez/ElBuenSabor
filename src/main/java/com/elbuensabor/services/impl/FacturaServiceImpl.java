package com.elbuensabor.services.impl;

import com.elbuensabor.dto.response.FacturaResponseDTO;
import com.elbuensabor.dto.response.PagoSummaryDTO;
import com.elbuensabor.entities.*;
import com.elbuensabor.exceptions.DuplicateResourceException;
import com.elbuensabor.exceptions.ResourceNotFoundException;
import com.elbuensabor.repository.IFacturaRepository;
import com.elbuensabor.services.IFacturaService;
import com.elbuensabor.services.mapper.FacturaMapper;
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

        // ✅ MAPEO MANUAL DE CAMPOS BÁSICOS
        dto.setIdFactura(factura.getIdFactura());
        dto.setFechaFactura(factura.getFechaFactura());
        dto.setNroComprobante(factura.getNroComprobante());
        dto.setSubTotal(factura.getSubTotal());
        dto.setDescuento(factura.getDescuento());
        dto.setGastosEnvio(factura.getGastosEnvio());
        dto.setTotalVenta(factura.getTotalVenta());

        // ✅ MAPEO SEGURO DE INFORMACIÓN DEL PEDIDO (SIN LAZY LOADING)
        if (factura.getPedido() != null) {
            Pedido pedido = factura.getPedido();
            dto.setPedidoId(pedido.getIdPedido());
            dto.setEstadoPedido(pedido.getEstado() != null ? pedido.getEstado().toString() : null);
            dto.setTipoEnvio(pedido.getTipoEnvio() != null ? pedido.getTipoEnvio().toString() : null);

            // ✅ INFORMACIÓN DEL CLIENTE (SIN LAZY LOADING)
            if (pedido.getCliente() != null) {
                dto.setClienteId(pedido.getCliente().getIdCliente());
                dto.setNombreCliente(pedido.getCliente().getNombre());
                dto.setApellidoCliente(pedido.getCliente().getApellido());
            }
        }

        // ✅ VALORES POR DEFECTO PARA PAGOS (SIN ACCEDER A LA COLECCIÓN)
        dto.setPagos(new ArrayList<>());
        dto.setTotalPagado(0.0);
        dto.setSaldoPendiente(factura.getTotalVenta());
        dto.setCompletamentePagada(false);

        return dto;
    }
}