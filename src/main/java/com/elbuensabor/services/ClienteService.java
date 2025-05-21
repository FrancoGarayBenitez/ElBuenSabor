package com.elbuensabor.services;

import com.elbuensabor.entities.Cliente;
import com.elbuensabor.services.base.GenericService;

import java.util.Optional;

public interface ClienteService extends GenericService<Cliente, Long> {
    Optional<Cliente> findByEmail(String email);
}
