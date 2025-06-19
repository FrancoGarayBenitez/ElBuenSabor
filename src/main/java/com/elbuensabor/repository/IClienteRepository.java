package com.elbuensabor.repository;

import com.elbuensabor.entities.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IClienteRepository extends JpaRepository<Cliente, Long> {

    @Query("SELECT COUNT(u) > 0 FROM Usuario u WHERE u.email = :email")
    boolean existsByUsuarioEmail(@Param("email") String email);

    Optional<Cliente> findByUsuarioEmail(String email);

    /**
     * Buscar cliente por Auth0 ID
     */
    Optional<Cliente> findByUsuario_Auth0Id(String auth0Id);

    /**
     * Verificar si existe un cliente con un Auth0 ID específico
     */
    boolean existsByUsuario_Auth0Id(String auth0Id);
}

