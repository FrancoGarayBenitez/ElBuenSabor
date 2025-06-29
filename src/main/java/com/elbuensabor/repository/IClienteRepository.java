package com.elbuensabor.repository;

import com.elbuensabor.entities.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IClienteRepository extends JpaRepository<Cliente, Long> {

    /**
     * Verifica si existe un usuario con el email dado
     */
    @Query("SELECT COUNT(u) > 0 FROM Usuario u WHERE u.email = :email")
    boolean existsByUsuarioEmail(@Param("email") String email);

    /**
     * Busca un cliente por su Auth0 ID con eager loading
     */
    @Query("SELECT c FROM Cliente c " +
            "LEFT JOIN FETCH c.domicilios " +
            "LEFT JOIN FETCH c.usuario " +
            "LEFT JOIN FETCH c.imagen " +
            "WHERE c.usuario.auth0Id = :auth0Id")
    Optional<Cliente> findByUsuarioAuth0Id(@Param("auth0Id") String auth0Id);

    /**
     * Busca un cliente por email de usuario con eager loading
     */
    @Query("SELECT c FROM Cliente c " +
            "LEFT JOIN FETCH c.domicilios " +
            "LEFT JOIN FETCH c.usuario " +
            "LEFT JOIN FETCH c.imagen " +
            "WHERE c.usuario.email = :email")
    Optional<Cliente> findByUsuarioEmail(@Param("email") String email);

    /**
     * Verifica si existe otro cliente con el mismo email (excluyendo el cliente actual)
     */
    @Query("SELECT COUNT(c) > 0 FROM Cliente c WHERE c.usuario.email = :email AND c.idCliente != :idCliente")
    boolean existsByUsuarioEmailAndIdClienteNot(@Param("email") String email, @Param("idCliente") Long idCliente);

    /**
     * Busca un cliente por Auth0 ID sin eager loading
     */
    @Query("SELECT c FROM Cliente c JOIN c.usuario u WHERE u.auth0Id = :auth0Id")
    Optional<Cliente> findByUsuarioAuth0IdLight(@Param("auth0Id") String auth0Id);

    /**
     * Verifica si un cliente es propietario de un domicilio específico
     */
    @Query("SELECT COUNT(d) > 0 FROM Domicilio d WHERE d.idDomicilio = :domicilioId AND d.cliente.idCliente = :clienteId")
    boolean isOwnerOfDomicilio(@Param("clienteId") Long clienteId, @Param("domicilioId") Long domicilioId);

    /**
     * Busca cliente por Auth0 ID y retorna solo el ID del cliente
     */
    @Query("SELECT c.idCliente FROM Cliente c JOIN c.usuario u WHERE u.auth0Id = :auth0Id")
    Optional<Long> findClienteIdByAuth0Id(@Param("auth0Id") String auth0Id);

    /**
     * Busca cliente por ID de usuario
     */
    Optional<Cliente> findByUsuarioIdUsuario(Long idUsuario);
}