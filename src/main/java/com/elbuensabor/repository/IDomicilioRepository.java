package com.elbuensabor.repository;

import com.elbuensabor.entities.Domicilio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IDomicilioRepository extends JpaRepository<Domicilio, Long> {
    // ✅ NUEVO: Buscar domicilios por cliente
    List<Domicilio> findByClienteIdCliente(Long clienteId);

    // ✅ OPCIONAL: Buscar domicilio de sucursal (donde id_cliente es NULL)
    List<Domicilio> findByClienteIsNull();
}
