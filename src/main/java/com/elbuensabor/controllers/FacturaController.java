package com.elbuensabor.controllers;

import com.elbuensabor.entities.Factura;
import com.elbuensabor.repository.IFacturaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/facturas")

public class FacturaController {

    @Autowired
    private IFacturaRepository facturaRepository;

    @GetMapping
    public ResponseEntity<List<Factura>> getAllFacturas() {
        List<Factura> facturas = facturaRepository.findAll();
        return ResponseEntity.ok(facturas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Factura> getFacturaById(@PathVariable Long id) {
        Optional<Factura> factura = facturaRepository.findById(id);
        if (factura.isPresent()) {
            return ResponseEntity.ok(factura.get());
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Factura> crearFactura(@RequestBody Factura factura) {
        Factura facturaGuardada = facturaRepository.save(factura);
        return ResponseEntity.ok(facturaGuardada);
    }
}