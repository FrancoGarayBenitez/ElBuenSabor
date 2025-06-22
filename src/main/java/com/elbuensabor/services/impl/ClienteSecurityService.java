package com.elbuensabor.services.impl;

import com.elbuensabor.entities.Cliente;
import com.elbuensabor.repository.IClienteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Servicio de seguridad para validar permisos de clientes
 * Utilizado en anotaciones @PreAuthorize para verificar propiedad de recursos
 */
@Service("clienteSecurityService")
public class ClienteSecurityService {

    private static final Logger logger = LoggerFactory.getLogger(ClienteSecurityService.class);

    @Autowired
    private IClienteRepository clienteRepository;

    /**
     * Verifica si el usuario autenticado es propietario del cliente con el ID dado
     *
     * @param auth0Id ID del usuario en Auth0 (viene de authentication.name)
     * @param clienteId ID del cliente que se quiere acceder
     * @return true si el usuario es propietario, false si no
     */
    public boolean isOwner(String auth0Id, Long clienteId) {
        try {
            logger.debug("Checking ownership for Auth0 user: {} and cliente ID: {}", auth0Id, clienteId);

            // Validar parámetros
            if (auth0Id == null || auth0Id.trim().isEmpty()) {
                logger.warn("Auth0 ID is null or empty");
                return false;
            }

            if (clienteId == null) {
                logger.warn("Cliente ID is null");
                return false;
            }

            // Buscar cliente por Auth0 ID
            Optional<Cliente> clienteByAuth0 = clienteRepository.findByUsuarioAuth0Id(auth0Id);
            if (clienteByAuth0.isEmpty()) {
                logger.debug("No cliente found for Auth0 ID: {}", auth0Id);
                return false;
            }

            // Verificar si el cliente encontrado tiene el mismo ID
            boolean isOwner = clienteByAuth0.get().getIdCliente().equals(clienteId);

            logger.debug("Ownership check result for user {} and cliente {}: {}",
                    auth0Id, clienteId, isOwner);

            return isOwner;

        } catch (Exception e) {
            logger.error("Error checking ownership for user {} and cliente {}: {}",
                    auth0Id, clienteId, e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si el usuario autenticado puede acceder a un pedido
     * (para uso futuro en PedidoController)
     *
     * @param auth0Id ID del usuario en Auth0
     * @param pedidoId ID del pedido
     * @return true si puede acceder, false si no
     */
    public boolean canAccessPedido(String auth0Id, Long pedidoId) {
        try {
            logger.debug("Checking pedido access for Auth0 user: {} and pedido ID: {}", auth0Id, pedidoId);

            // Buscar cliente por Auth0 ID
            Optional<Cliente> clienteByAuth0 = clienteRepository.findByUsuarioAuth0Id(auth0Id);
            if (clienteByAuth0.isEmpty()) {
                return false;
            }

            // Verificar si el pedido pertenece al cliente
            Cliente cliente = clienteByAuth0.get();
            boolean canAccess = cliente.getPedidos().stream()
                    .anyMatch(pedido -> pedido.getIdPedido().equals(pedidoId));

            logger.debug("Pedido access check result for user {} and pedido {}: {}",
                    auth0Id, pedidoId, canAccess);

            return canAccess;

        } catch (Exception e) {
            logger.error("Error checking pedido access for user {} and pedido {}: {}",
                    auth0Id, pedidoId, e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene el ID del cliente asociado al usuario Auth0
     * Útil para operaciones internas
     *
     * @param auth0Id ID del usuario en Auth0
     * @return ID del cliente o null si no se encuentra
     */
    public Long getClienteIdByAuth0Id(String auth0Id) {
        try {
            if (auth0Id == null || auth0Id.trim().isEmpty()) {
                return null;
            }

            Optional<Cliente> cliente = clienteRepository.findByUsuarioAuth0Id(auth0Id);
            return cliente.map(Cliente::getIdCliente).orElse(null);

        } catch (Exception e) {
            logger.error("Error getting cliente ID for Auth0 user {}: {}", auth0Id, e.getMessage());
            return null;
        }
    }
}