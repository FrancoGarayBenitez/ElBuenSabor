package com.elbuensabor.controllers;

import com.elbuensabor.dto.response.MovimientosMonetariosDTO;
import com.elbuensabor.dto.response.RankingProductoDTO;
import com.elbuensabor.services.IEstadisticasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/estadisticas")
public class EstadisticasController {

    @Autowired
    private IEstadisticasService estadisticasService;

    @GetMapping("/ranking-productos")
    public ResponseEntity<List<RankingProductoDTO>> getRankingProductos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(defaultValue = "10") Integer limit) {

        List<RankingProductoDTO> ranking = estadisticasService.findRankingProductos(fechaDesde, fechaHasta, limit);
        return ResponseEntity.ok(ranking);
    }
    @GetMapping("/movimientos")
    public ResponseEntity<MovimientosMonetariosDTO> getMovimientosMonetarios(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {

        MovimientosMonetariosDTO movimientos = estadisticasService.findMovimientosMonetarios(fechaDesde, fechaHasta);
        return ResponseEntity.ok(movimientos);
    }
}
