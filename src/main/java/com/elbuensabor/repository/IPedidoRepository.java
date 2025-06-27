package com.elbuensabor.repository;

import com.elbuensabor.dto.response.RankingProductoDTO;
import com.elbuensabor.entities.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable; // <-- LA CORRECTA
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IPedidoRepository extends JpaRepository<Pedido, Long> {

    // Buscar por cliente
    List<Pedido> findByClienteIdClienteOrderByFechaDesc(Long idCliente);

    // Buscar por estado
    List<Pedido> findByEstadoOrderByFechaAsc(Estado estado);

    // Buscar por estado y tipo de envío
    List<Pedido> findByEstadoAndTipoEnvioOrderByFechaAsc(Estado estado, TipoEnvio tipoEnvio);

    // Buscar pedidos de hoy
    @Query("SELECT p FROM Pedido p WHERE DATE(p.fecha) = CURRENT_DATE ORDER BY p.fecha ASC")
    List<Pedido> findPedidosDeHoy();

    // Buscar pedidos por rango de fechas
    @Query("SELECT p FROM Pedido p WHERE p.fecha BETWEEN :fechaInicio AND :fechaFin ORDER BY p.fecha ASC")
    List<Pedido> findByFechaBetween(@Param("fechaInicio") LocalDateTime fechaInicio,
                                    @Param("fechaFin") LocalDateTime fechaFin);
    @Query("SELECT new com.elbuensabor.dto.response.RankingProductoDTO(dp.articulo.idArticulo, dp.articulo.denominacion, SUM(dp.cantidad)) " +
            "FROM Pedido p JOIN p.detalles dp " +
            "WHERE p.fecha BETWEEN :fechaDesde AND :fechaHasta " +
            "AND p.estado <> com.elbuensabor.entities.Estado.CANCELADO " + // Opcional: excluir pedidos cancelados
            "GROUP BY dp.articulo.idArticulo, dp.articulo.denominacion " +
            "ORDER BY SUM(dp.cantidad) DESC")
    List<RankingProductoDTO> findRankingProductosVendidos(
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            Pageable pageable // Para limitar los resultados (ej. Top 10)
    );
    @Query("SELECT SUM(p.total), SUM(p.totalCosto) " +
            "FROM Pedido p " +
            "WHERE p.fecha BETWEEN :fechaDesde AND :fechaHasta " +
            "AND p.estado <> com.elbuensabor.entities.Estado.CANCELADO")
    List<Object[]> findMovimientosMonetarios(
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta
    );
}