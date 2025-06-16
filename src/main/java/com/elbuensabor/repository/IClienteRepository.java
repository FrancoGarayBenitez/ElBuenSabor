package com.elbuensabor.repository;

import com.elbuensabor.entities.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IClienteRepository extends JpaRepository<Cliente, Long> {

    @Query("SELECT COUNT(u) > 0 FROM Usuario u WHERE u.email = :email")
    boolean existsByUsuarioEmail(@Param("email") String email);

    @Query("SELECT c FROM Cliente c WHERE c.usuario.auth0Id = :auth0Id")
    Optional<Cliente> findByUsuarioAuth0Id(@Param("auth0Id") String auth0Id);

    Optional<Cliente> findByUsuarioEmail(String email);

    Optional<Cliente> findByUsuarioAuth0Id(String auth0Id);
}

