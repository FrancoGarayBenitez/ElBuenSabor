package com.elbuensabor.services.impl;

import com.elbuensabor.dto.response.MovimientosMonetariosDTO;
import com.elbuensabor.dto.response.RankingProductoDTO;
import com.elbuensabor.repository.IPedidoRepository;
import com.elbuensabor.services.IEstadisticasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class EstadisticasServiceImpl implements IEstadisticasService {

    @Autowired
    private IPedidoRepository pedidoRepository;

    @Override
    public List<RankingProductoDTO> findRankingProductos(LocalDate fechaDesde, LocalDate fechaHasta, Integer limit) {
        LocalDateTime inicio = fechaDesde.atStartOfDay();
        LocalDateTime fin = fechaHasta.atTime(LocalTime.MAX);
        Pageable pageable = PageRequest.of(0, limit);

        return pedidoRepository.findRankingProductosVendidos(inicio, fin, pageable);
    }
    @Override
    public MovimientosMonetariosDTO findMovimientosMonetarios(LocalDate fechaDesde, LocalDate fechaHasta) {
        LocalDateTime inicio = fechaDesde.atStartOfDay();
        LocalDateTime fin = fechaHasta.atTime(LocalTime.MAX);

        List<Object[]> resultados = pedidoRepository.findMovimientosMonetarios(inicio, fin);
        Object[] resultado = resultados.get(0);

        Double ingresos = (Double) (resultado[0] != null ? resultado[0] : 0.0);
        Double costos = (Double) (resultado[1] != null ? resultado[1] : 0.0);
        Double ganancias = ingresos - costos;

        return new MovimientosMonetariosDTO(ingresos, costos, ganancias);
    }
}
