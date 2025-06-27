package com.elbuensabor.services.impl;

import com.elbuensabor.dto.request.ClienteRegisterDTO;
import com.elbuensabor.dto.response.ClienteResponseDTO;
import com.elbuensabor.entities.Cliente;
import com.elbuensabor.entities.Rol;
import com.elbuensabor.entities.Usuario;
import com.elbuensabor.exceptions.DuplicateResourceException;
import com.elbuensabor.exceptions.ResourceNotFoundException;
import com.elbuensabor.repository.IClienteRepository;
import com.elbuensabor.services.IClienteService;
import com.elbuensabor.services.mapper.ClienteMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
public class ClienteServiceImpl extends GenericServiceImpl<Cliente, Long, ClienteResponseDTO, IClienteRepository, ClienteMapper>
        implements IClienteService {

    private static final Logger logger = LoggerFactory.getLogger(ClienteServiceImpl.class);

    @Autowired
    public ClienteServiceImpl(IClienteRepository repository, ClienteMapper mapper) {
        super(repository, mapper, Cliente.class, ClienteResponseDTO.class);
    }
    @Override
    public boolean existsByAuth0Id(String auth0Id) {
        return repository.existsByUsuarioAuth0Id(auth0Id);
    }


    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO findByAuth0Id(String auth0Id) {
        logger.debug("Finding cliente by Auth0 ID: {}", auth0Id);

        if (auth0Id == null || auth0Id.trim().isEmpty()) {
            throw new IllegalArgumentException("Auth0 ID no puede ser null o vacío");
        }

        Cliente cliente = repository.findByUsuarioAuth0Id(auth0Id)
                .orElseThrow(() -> {
                    logger.warn("Cliente not found for Auth0 ID: {}", auth0Id);
                    return new ResourceNotFoundException("Cliente no encontrado para el usuario autenticado");
                });

        logger.debug("Found cliente with ID: {} for Auth0 ID: {}", cliente.getIdCliente(), auth0Id);
        return mapper.toDTO(cliente);
    }


    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO findByEmail(String email) {
        logger.debug("Finding cliente by email: {}", email);

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email no puede ser null o vacío");
        }

        Cliente cliente = repository.findByUsuarioEmail(email)
                .orElseThrow(() -> {
                    logger.warn("Cliente not found for email: {}", email);
                    return new ResourceNotFoundException("Cliente no encontrado con email: " + email);
                });

        logger.debug("Found cliente with ID: {} for email: {}", cliente.getIdCliente(), email);
        return mapper.toDTO(cliente);
    }

    @Override
    @Transactional
    public ClienteResponseDTO update(Long id, ClienteResponseDTO clienteDTO) {
        logger.debug("Updating cliente with ID: {}", id);

        // Buscar cliente existente
        Cliente existingCliente = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));

        // Actualizar solo los campos permitidos (no tocar usuario/auth0)
        updateClienteFields(existingCliente, clienteDTO);

        // Guardar cambios
        Cliente updatedCliente = repository.save(existingCliente);

        logger.debug("Cliente updated successfully with ID: {}", id);
        return mapper.toDTO(updatedCliente);
    }

    /**
     * Actualiza los campos del cliente manteniendo la información de usuario intacta
     * No permite cambios en datos de autenticación (Usuario/Auth0)
     */
    private void updateClienteFields(Cliente existingCliente, ClienteResponseDTO clienteDTO) {
        // Actualizar datos básicos del cliente
        if (clienteDTO.getNombre() != null && !clienteDTO.getNombre().trim().isEmpty()) {
            existingCliente.setNombre(clienteDTO.getNombre().trim());
        }

        if (clienteDTO.getApellido() != null && !clienteDTO.getApellido().trim().isEmpty()) {
            existingCliente.setApellido(clienteDTO.getApellido().trim());
        }

        if (clienteDTO.getTelefono() != null) {
            existingCliente.setTelefono(clienteDTO.getTelefono().trim());
        }

        if (clienteDTO.getFechaNacimiento() != null) {
            existingCliente.setFechaNacimiento(clienteDTO.getFechaNacimiento());
        }

        // Actualizar email en Usuario si es diferente y válido
        if (clienteDTO.getEmail() != null &&
                !clienteDTO.getEmail().trim().isEmpty() &&
                !clienteDTO.getEmail().equals(existingCliente.getUsuario().getEmail())) {

            // Verificar que el nuevo email no esté en uso por otro cliente
            if (repository.existsByUsuarioEmailAndIdClienteNot(clienteDTO.getEmail(), existingCliente.getIdCliente())) {
                throw new IllegalArgumentException("El email ya está en uso por otro cliente");
            }

            existingCliente.getUsuario().setEmail(clienteDTO.getEmail().trim());
            logger.debug("Updated email for cliente ID: {}", existingCliente.getIdCliente());
        }


        // TODO: Actualizar domicilios e imagen si están presentes en el DTO
        // Por ahora se mantiene la lógica simple, se puede extender después

        logger.debug("Updated fields for cliente ID: {}", existingCliente.getIdCliente());
    }

}