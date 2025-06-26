package com.elbuensabor.services.impl;

import com.elbuensabor.dto.request.CompraInsumoRequestDTO;
import com.elbuensabor.entities.ArticuloInsumo;
import com.elbuensabor.entities.CompraInsumo;
import com.elbuensabor.exceptions.ResourceNotFoundException;
import com.elbuensabor.repository.IArticuloInsumoRepository;
import com.elbuensabor.repository.ICompraInsumoRepository;
import com.elbuensabor.services.CompraInsumoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompraInsumoServiceImpl implements CompraInsumoService {

    private final ICompraInsumoRepository compraInsumoRepository;
    private final IArticuloInsumoRepository articuloInsumoRepository;

    @Override
    public void registrarCompra(CompraInsumoRequestDTO dto) {
        ArticuloInsumo insumo = articuloInsumoRepository.findById(dto.getInsumoId())
                .orElseThrow(() -> new ResourceNotFoundException("Insumo no encontrado con ID: " + dto.getInsumoId()));

        CompraInsumo compra = new CompraInsumo();
        compra.setInsumo(insumo);
        compra.setCantidad(dto.getCantidad());
        compra.setPrecioUnitario(dto.getPrecioUnitario());
        compra.setFechaCompra(dto.getFechaCompra());

        insumo.setStockActual(insumo.getStockActual() + dto.getCantidad().intValue());

        compraInsumoRepository.save(compra);
        articuloInsumoRepository.save(insumo);
    }
}
