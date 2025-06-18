package com.elbuensabor.services.impl;

import com.elbuensabor.dto.request.PedidoRequestDTO;
import com.elbuensabor.dto.response.PedidoResponseDTO;
import com.elbuensabor.entities.*;
import com.elbuensabor.exceptions.ResourceNotFoundException;
import com.elbuensabor.repository.*;
import com.elbuensabor.services.IPedidoService;
import com.elbuensabor.services.mapper.PedidoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PedidoServiceImpl implements IPedidoService {

    @Autowired
    private IPedidoRepository pedidoRepository;

    @Autowired
    private IClienteRepository clienteRepository;

    @Autowired
    private IDomicilioRepository domicilioRepository;

    @Autowired
    private IArticuloManufacturadoRepository articuloRepository;

    @Autowired
    private IArticuloInsumoRepository articuloInsumoRepository;

    @Autowired
    private PedidoMapper pedidoMapper;

    @Autowired
    private ISucursalEmpresaRepository sucursalRepository;

    private PedidoResponseDTO enrichPedidoResponse(Pedido pedido) {
        PedidoResponseDTO response = pedidoMapper.toDTO(pedido);

        // Calcular stock (siempre true para pedidos ya creados)
        response.setStockSuficiente(true);

        // Calcular tiempo estimado desde los detalles
        response.setTiempoEstimadoTotal(calcularTiempoEstimadoDesdeDetalles(pedido.getDetalles()));

        return response;
    }

    private Integer calcularTiempoEstimadoDesdeDetalles(List<DetallePedido> detalles) {
        int tiempoMaximo = 0;

        for (DetallePedido detalle : detalles) {
            if (detalle.getArticulo() instanceof ArticuloManufacturado) {
                ArticuloManufacturado manufacturado = (ArticuloManufacturado) detalle.getArticulo();
                if (manufacturado.getTiempoEstimadoEnMinutos() != null) {
                    tiempoMaximo = Math.max(tiempoMaximo, manufacturado.getTiempoEstimadoEnMinutos());
                }
            }
        }

        return tiempoMaximo > 0 ? tiempoMaximo : null;
    }

    @Override
    @Transactional
    public PedidoResponseDTO crearPedido(PedidoRequestDTO pedidoRequest) {
        // 1. Validar cliente
        System.out.println("🚀 RECIBIENDO PEDIDO REQUEST:");
        System.out.println("📝 Observaciones recibidas: '" + pedidoRequest.getObservaciones() + "'");
        Cliente cliente = clienteRepository.findById(pedidoRequest.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        SucursalEmpresa sucursal = sucursalRepository.findById(pedidoRequest.getIdSucursal())
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

        // 2. Validar stock disponible
        if (!validarStockDisponible(pedidoRequest)) {
            throw new IllegalArgumentException("Stock insuficiente para algunos productos");
        }

        // 3. Crear entidad Pedido
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setSucursal(sucursal);
        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado(Estado.PENDIENTE);
        pedido.setTipoEnvio(TipoEnvio.valueOf(pedidoRequest.getTipoEnvio()));
        pedido.setObservaciones(pedidoRequest.getObservaciones());
        System.out.println("💾 Observaciones asignadas a entidad: '" + pedido.getObservaciones() + "'");

        // 4. Asignar domicilio si es delivery

        if (pedidoRequest.getTipoEnvio().equals("DELIVERY")) {
            if (pedidoRequest.getIdDomicilio() == null) {
                throw new IllegalArgumentException("Domicilio requerido para delivery");
            }
            Domicilio domicilio = domicilioRepository.findById(pedidoRequest.getIdDomicilio())
                    .orElseThrow(() -> new ResourceNotFoundException("Domicilio no encontrado"));
            pedido.setDomicilio(domicilio);
        }

        // 5. Calcular totales
        Double total = calcularTotal(pedidoRequest);
        Double totalCosto = calcularTotalCosto(pedidoRequest);
        pedido.setTotal(total);
        pedido.setTotalCosto(totalCosto);

        // 6. Calcular tiempo estimado
        Integer tiempoEstimado = calcularTiempoEstimado(pedidoRequest);
        LocalTime horaEstimada = LocalTime.now().plusMinutes(tiempoEstimado);
        pedido.setHoraEstimadaFinalizacion(horaEstimada);

        // 7. Guardar pedido
        Pedido pedidoGuardado = pedidoRepository.save(pedido);
        System.out.println("💾 Observaciones asignadas a entidad: '" + pedido.getObservaciones() + "'");
        // 8. Crear detalles del pedido
        List<DetallePedido> detalles = pedidoRequest.getDetalles().stream()
                .map(detalleRequest -> {
                    Articulo articulo = articuloRepository.findById(detalleRequest.getIdArticulo())
                            .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado"));

                    DetallePedido detalle = new DetallePedido();
                    detalle.setPedido(pedidoGuardado);
                    detalle.setArticulo(articulo);
                    detalle.setCantidad(detalleRequest.getCantidad());
                    detalle.setSubtotal(articulo.getPrecioVenta() * detalleRequest.getCantidad());

                    return detalle;
                })
                .collect(Collectors.toList());

        pedidoGuardado.setDetalles(detalles);

        // 9. Actualizar stock de ingredientes - NO ACTUALIZAR HASTA CONFIRMAR
        // actualizarStockIngredientes(pedidoRequest);

        // 10. Guardar con detalles
        Pedido pedidoFinal = pedidoRepository.save(pedidoGuardado);

        // 11. Mapear a DTO
        PedidoResponseDTO response = pedidoMapper.toDTO(pedidoFinal);
        System.out.println("📤 Observaciones en response: '" + response.getObservaciones() + "'");

        // 12. Calcular campos faltantes
        response.setStockSuficiente(validarStockDisponible(pedidoRequest));
        response.setTiempoEstimadoTotal(calcularTiempoEstimado(pedidoRequest));

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PedidoResponseDTO findById(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
        return enrichPedidoResponse(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> findAll() {
        return pedidoRepository.findAll().stream()
                .map(this::enrichPedidoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> findByCliente(Long idCliente) {
        return pedidoRepository.findByClienteIdClienteOrderByFechaDesc(idCliente).stream()
                .map(this::enrichPedidoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PedidoResponseDTO confirmarPedido(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

        if (pedido.getEstado() != Estado.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden confirmar pedidos pendientes");
        }

        // Al confirmar, cambiar a PREPARACION y actualizar stock
        pedido.setEstado(Estado.PREPARACION);
        actualizarStockDesdePedido(pedido);

        Pedido pedidoActualizado = pedidoRepository.save(pedido);
        return enrichPedidoResponse(pedidoActualizado);
    }

    @Override
    @Transactional
    public PedidoResponseDTO marcarEnPreparacion(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

        if (pedido.getEstado() != Estado.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden pasar a preparación pedidos pendientes");
        }

        pedido.setEstado(Estado.PREPARACION);
        actualizarStockDesdePedido(pedido);

        Pedido pedidoActualizado = pedidoRepository.save(pedido);
        return enrichPedidoResponse(pedidoActualizado);
    }

    @Override
    @Transactional
    public PedidoResponseDTO marcarListo(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

        if (pedido.getEstado() != Estado.PREPARACION) {
            throw new IllegalStateException("El pedido debe estar en preparación para marcarlo como listo");
        }

        pedido.setEstado(Estado.LISTO);
        Pedido pedidoActualizado = pedidoRepository.save(pedido);
        return enrichPedidoResponse(pedidoActualizado);
    }

    @Override
    @Transactional
    public PedidoResponseDTO marcarEntregado(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

        // Verificar que esté listo o en preparación (para take away)
        if (pedido.getEstado() != Estado.LISTO &&
                !(pedido.getEstado() == Estado.PREPARACION && pedido.getTipoEnvio() == TipoEnvio.TAKE_AWAY)) {
            throw new IllegalStateException("El pedido debe estar listo para ser entregado");
        }

        pedido.setEstado(Estado.ENTREGADO);
        Pedido pedidoActualizado = pedidoRepository.save(pedido);
        return enrichPedidoResponse(pedidoActualizado);
    }

    @Override
    @Transactional
    public PedidoResponseDTO cancelarPedido(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

        if (pedido.getEstado() == Estado.ENTREGADO) {
            throw new IllegalStateException("No se puede cancelar un pedido entregado");
        }

        // Restaurar stock solo si el pedido estaba en preparación o listo
        if (pedido.getEstado() == Estado.PREPARACION || pedido.getEstado() == Estado.LISTO) {
            restaurarStockIngredientes(pedido);
        }

        pedido.setEstado(Estado.CANCELADO);
        Pedido pedidoActualizado = pedidoRepository.save(pedido);
        return enrichPedidoResponse(pedidoActualizado);
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean validarStockDisponible(PedidoRequestDTO pedidoRequest) {
        for (var detalle : pedidoRequest.getDetalles()) {
            Articulo articulo = articuloRepository.findById(detalle.getIdArticulo())
                    .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado"));

            if (articulo instanceof ArticuloManufacturado) {
                ArticuloManufacturado manufacturado = (ArticuloManufacturado) articulo;

                // Verificar stock de cada ingrediente
                for (var ingrediente : manufacturado.getDetalles()) {
                    double cantidadNecesaria = ingrediente.getCantidad() * detalle.getCantidad();
                    if (ingrediente.getArticuloInsumo().getStockActual() < cantidadNecesaria) {
                        return false;
                    }
                }
            } else if (articulo instanceof ArticuloInsumo) {
                ArticuloInsumo insumo = (ArticuloInsumo) articulo;
                if (insumo.getStockActual() < detalle.getCantidad()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Double calcularTotal(PedidoRequestDTO pedidoRequest) {
        double subtotal = 0;

        for (var detalle : pedidoRequest.getDetalles()) {
            Articulo articulo = articuloRepository.findById(detalle.getIdArticulo())
                    .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado"));

            subtotal += articulo.getPrecioVenta() * detalle.getCantidad();
        }

        // Agregar costo de envío si es delivery
        if ("DELIVERY".equals(pedidoRequest.getTipoEnvio())) {
            subtotal += 200; // Costo fijo de delivery
        }

        return subtotal;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer calcularTiempoEstimado(PedidoRequestDTO pedidoRequest) {
        int tiempoMaximo = 0;

        for (var detalle : pedidoRequest.getDetalles()) {
            Articulo articulo = articuloRepository.findById(detalle.getIdArticulo())
                    .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado"));

            if (articulo instanceof ArticuloManufacturado) {
                ArticuloManufacturado manufacturado = (ArticuloManufacturado) articulo;
                tiempoMaximo = Math.max(tiempoMaximo, manufacturado.getTiempoEstimadoEnMinutos());
            }
        }

        // Agregar tiempo de delivery si corresponde
        if ("DELIVERY".equals(pedidoRequest.getTipoEnvio())) {
            tiempoMaximo += 15; // Tiempo estimado de entrega
        }

        return tiempoMaximo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> findPedidosPendientes() {
        return pedidoRepository.findByEstadoOrderByFechaAsc(Estado.PENDIENTE).stream()
                .map(this::enrichPedidoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> findPedidosEnPreparacion() {
        return pedidoRepository.findByEstadoOrderByFechaAsc(Estado.PREPARACION).stream()
                .map(this::enrichPedidoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> findPedidosListos() {
        return pedidoRepository.findByEstadoOrderByFechaAsc(Estado.LISTO).stream()
                .map(this::enrichPedidoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> findPedidosListosParaEntrega() {
        // Pedidos listos para delivery
        return pedidoRepository.findByEstadoAndTipoEnvioOrderByFechaAsc(Estado.LISTO, TipoEnvio.DELIVERY).stream()
                .map(this::enrichPedidoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> findPedidosListosParaRetiro() {
        // Pedidos listos para take away
        return pedidoRepository.findByEstadoAndTipoEnvioOrderByFechaAsc(Estado.LISTO, TipoEnvio.TAKE_AWAY).stream()
                .map(this::enrichPedidoResponse)
                .collect(Collectors.toList());
    }

    // Métodos auxiliares privados
    private Double calcularTotalCosto(PedidoRequestDTO pedidoRequest) {
        double totalCosto = 0;

        for (var detalle : pedidoRequest.getDetalles()) {
            Articulo articulo = articuloRepository.findById(detalle.getIdArticulo())
                    .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado"));

            if (articulo instanceof ArticuloManufacturado) {
                ArticuloManufacturado manufacturado = (ArticuloManufacturado) articulo;
                double costoUnitario = manufacturado.getDetalles().stream()
                        .mapToDouble(ing -> ing.getCantidad() * ing.getArticuloInsumo().getPrecioCompra())
                        .sum();
                totalCosto += costoUnitario * detalle.getCantidad();
            } else if (articulo instanceof ArticuloInsumo) {
                ArticuloInsumo insumo = (ArticuloInsumo) articulo;
                totalCosto += insumo.getPrecioCompra() * detalle.getCantidad();
            }
        }

        return totalCosto;
    }

    private void actualizarStockIngredientes(PedidoRequestDTO pedidoRequest) {
        for (var detalle : pedidoRequest.getDetalles()) {
            Articulo articulo = articuloRepository.findById(detalle.getIdArticulo())
                    .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado"));

            if (articulo instanceof ArticuloManufacturado) {
                ArticuloManufacturado manufacturado = (ArticuloManufacturado) articulo;

                for (var ingrediente : manufacturado.getDetalles()) {
                    ArticuloInsumo insumo = ingrediente.getArticuloInsumo();
                    int cantidadARestar = (int) (ingrediente.getCantidad() * detalle.getCantidad());
                    insumo.setStockActual(insumo.getStockActual() - cantidadARestar);
                    articuloInsumoRepository.save(insumo);
                }
            } else if (articulo instanceof ArticuloInsumo) {
                ArticuloInsumo insumo = (ArticuloInsumo) articulo;
                insumo.setStockActual(insumo.getStockActual() - detalle.getCantidad());
                articuloInsumoRepository.save(insumo);
            }
        }
    }

    private void actualizarStockDesdePedido(Pedido pedido) {
        for (var detalle : pedido.getDetalles()) {
            Articulo articulo = detalle.getArticulo();

            if (articulo instanceof ArticuloManufacturado) {
                ArticuloManufacturado manufacturado = (ArticuloManufacturado) articulo;

                for (var ingrediente : manufacturado.getDetalles()) {
                    ArticuloInsumo insumo = ingrediente.getArticuloInsumo();
                    int cantidadARestar = (int) (ingrediente.getCantidad() * detalle.getCantidad());
                    insumo.setStockActual(insumo.getStockActual() - cantidadARestar);
                    articuloInsumoRepository.save(insumo);
                }
            } else if (articulo instanceof ArticuloInsumo) {
                ArticuloInsumo insumo = (ArticuloInsumo) articulo;
                insumo.setStockActual(insumo.getStockActual() - detalle.getCantidad());
                articuloInsumoRepository.save(insumo);
            }
        }
    }

    private void restaurarStockIngredientes(Pedido pedido) {
        for (var detalle : pedido.getDetalles()) {
            Articulo articulo = detalle.getArticulo();

            if (articulo instanceof ArticuloManufacturado) {
                ArticuloManufacturado manufacturado = (ArticuloManufacturado) articulo;

                for (var ingrediente : manufacturado.getDetalles()) {
                    ArticuloInsumo insumo = ingrediente.getArticuloInsumo();
                    int cantidadARestaurar = (int) (ingrediente.getCantidad() * detalle.getCantidad());
                    insumo.setStockActual(insumo.getStockActual() + cantidadARestaurar);
                    articuloInsumoRepository.save(insumo);
                }
            } else if (articulo instanceof ArticuloInsumo) {
                ArticuloInsumo insumo = (ArticuloInsumo) articulo;
                insumo.setStockActual(insumo.getStockActual() + detalle.getCantidad());
                articuloInsumoRepository.save(insumo);
            }
        }
    }
}