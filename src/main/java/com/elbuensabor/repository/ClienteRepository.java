package com.elbuensabor.repository;

import com.elbuensabor.entities.Cliente;
import com.elbuensabor.repository.base.GenericRepository;

import java.util.Optional;

public interface ClienteRepository extends GenericRepository<Cliente, Long> {
    Optional<Cliente> findByEmail(String email);
}

