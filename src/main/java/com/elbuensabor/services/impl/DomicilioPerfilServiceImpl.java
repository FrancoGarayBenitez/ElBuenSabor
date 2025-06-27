package com.elbuensabor.services.impl;

import com.elbuensabor.dto.request.DomicilioRequestDTO;
import com.elbuensabor.dto.response.DomicilioResponseDTO;
import com.elbuensabor.entities.Cliente;
import com.elbuensabor.entities.Domicilio;
import com.elbuensabor.exceptions.ResourceNotFoundException;
import com.elbuensabor.repository.IClienteRepository;
import com.elbuensabor.repository.IDomicilioRepository;
import com.elbuensabor.services.IDomicilioPerfilService;
import com.elbuensabor.services.mapper.DomicilioMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de domicilios para perfil de usuario
 * Maneja todas las operaciones de domicilios restringidas al usuario autenticado
 */
@Service
public class DomicilioPerfilServiceImpl implements IDomicilioPerfilService {

    private static final Logger logger = LoggerFactory.getLogger(DomicilioPerfilServiceImpl.class);

    private final IDomicilioRepository domicilioRepository;
    private final IClienteRepository clienteRepository;
    private final DomicilioMapper domicilioMapper;

    @Autowired
    public DomicilioPerfilServiceImpl(IDomicilioRepository domicilioRepository,
                                      IClienteRepository clienteRepository,
                                      DomicilioMapper domicilioMapper) {
        this.domicilioRepository = domicilioRepository;
        this.clienteRepository = clienteRepository;
        this.domicilioMapper = domicilioMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomicilioResponseDTO> getMisDomicilios(String auth0Id) {
        logger.debug("Getting domicilios for user: {}", auth0Id);

        Long clienteId = getClienteIdByAuth0Id(auth0Id);
        List<Domicilio> domicilios = domicilioRepository.findByClienteIdOrderByPrincipal(clienteId);

        logger.debug("Found {} domicilios for cliente ID: {}", domicilios.size(), clienteId);
        return domicilios.stream()
                .map(domicilioMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DomicilioResponseDTO getMiDomicilioPrincipal(String auth0Id) {
        logger.debug("Getting principal domicilio for user: {}", auth0Id);

        Long clienteId = getClienteIdByAuth0Id(auth0Id);
        return domicilioRepository.findPrincipalByClienteId(clienteId)
                .map(domicilioMapper::toResponseDTO)
                .orElse(null);
    }

    @Override
    @Transactional
    public DomicilioResponseDTO crearMiDomicilio(String auth0Id, DomicilioRequestDTO domicilioDTO) {
        logger.debug("Creating new domicilio for user: {}", auth0Id);

        Cliente cliente = getClienteByAuth0Id(auth0Id);

        // Si será principal, quitar principal de los demás
        if (Boolean.TRUE.equals(domicilioDTO.getEsPrincipal())) {
            domicilioRepository.clearPrincipalForCliente(cliente.getIdCliente());
            logger.debug("Cleared principal status for other domicilios of cliente: {}", cliente.getIdCliente());
        }
        // Si es el primer domicilio, marcarlo como principal automáticamente
        else if (domicilioRepository.countByClienteId(cliente.getIdCliente()) == 0) {
            domicilioDTO.setEsPrincipal(true);
            logger.debug("First domicilio for cliente {}, marking as principal", cliente.getIdCliente());
        }

        // Crear nuevo domicilio
        Domicilio nuevoDomicilio = domicilioMapper.toEntity(domicilioDTO);
        nuevoDomicilio.setCliente(cliente);

        Domicilio domicilioGuardado = domicilioRepository.save(nuevoDomicilio);
        logger.debug("Created domicilio with ID: {} for cliente: {}", domicilioGuardado.getIdDomicilio(), cliente.getIdCliente());

        return domicilioMapper.toResponseDTO(domicilioGuardado);
    }

    @Override
    @Transactional
    public DomicilioResponseDTO actualizarMiDomicilio(String auth0Id, Long domicilioId, DomicilioRequestDTO domicilioDTO) {
        logger.debug("Updating domicilio {} for user: {}", domicilioId, auth0Id);

        Long clienteId = getClienteIdByAuth0Id(auth0Id);

        // Validar que el domicilio pertenezca al usuario
        Domicilio domicilioExistente = domicilioRepository.findByIdAndClienteId(domicilioId, clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Domicilio no encontrado o no pertenece al usuario"));

        // Si será principal, quitar principal de los demás
        if (Boolean.TRUE.equals(domicilioDTO.getEsPrincipal()) && !domicilioExistente.getEsPrincipal()) {
            domicilioRepository.clearPrincipalForCliente(clienteId);
            logger.debug("Cleared principal status for other domicilios of cliente: {}", clienteId);
        }

        // Actualizar campos
        domicilioMapper.updateEntityFromDTO(domicilioDTO, domicilioExistente);

        Domicilio domicilioActualizado = domicilioRepository.save(domicilioExistente);
        logger.debug("Updated domicilio with ID: {}", domicilioActualizado.getIdDomicilio());

        return domicilioMapper.toResponseDTO(domicilioActualizado);
    }

    @Override
    @Transactional
    public void eliminarMiDomicilio(String auth0Id, Long domicilioId) {
        logger.debug("Deleting domicilio {} for user: {}", domicilioId, auth0Id);

        Long clienteId = getClienteIdByAuth0Id(auth0Id);

        // Validar que el domicilio pertenezca al usuario
        Domicilio domicilio = domicilioRepository.findByIdAndClienteId(domicilioId, clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Domicilio no encontrado o no pertenece al usuario"));

        // Verificar que no sea el último domicilio
        long cantidadDomicilios = domicilioRepository.countByClienteId(clienteId);
        if (cantidadDomicilios <= 1) {
            throw new IllegalStateException("No se puede eliminar el último domicilio. El cliente debe tener al menos uno.");
        }

        domicilioRepository.delete(domicilio);
        logger.debug("Deleted domicilio with ID: {} for cliente: {}", domicilioId, clienteId);

        // Nota: No marcamos automáticamente otro como principal si se elimina el principal
        // El frontend puede manejar esto o se puede agregar lógica aquí si se prefiere
    }

    @Override
    @Transactional
    public DomicilioResponseDTO marcarComoPrincipal(String auth0Id, Long domicilioId) {
        logger.debug("Marking domicilio {} as principal for user: {}", domicilioId, auth0Id);

        Long clienteId = getClienteIdByAuth0Id(auth0Id);

        // Validar que el domicilio pertenezca al usuario
        Domicilio domicilio = domicilioRepository.findByIdAndClienteId(domicilioId, clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Domicilio no encontrado o no pertenece al usuario"));

        // Quitar principal de todos los domicilios del cliente
        domicilioRepository.clearPrincipalForCliente(clienteId);

        // Marcar este como principal
        domicilio.setEsPrincipal(true);
        Domicilio domicilioActualizado = domicilioRepository.save(domicilio);

        logger.debug("Marked domicilio {} as principal for cliente: {}", domicilioId, clienteId);
        return domicilioMapper.toResponseDTO(domicilioActualizado);
    }

    @Override
    @Transactional(readOnly = true)
    public DomicilioResponseDTO getMiDomicilio(String auth0Id, Long domicilioId) {
        logger.debug("Getting domicilio {} for user: {}", domicilioId, auth0Id);

        Long clienteId = getClienteIdByAuth0Id(auth0Id);

        Domicilio domicilio = domicilioRepository.findByIdAndClienteId(domicilioId, clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Domicilio no encontrado o no pertenece al usuario"));

        return domicilioMapper.toResponseDTO(domicilio);
    }

    @Override
    @Transactional(readOnly = true)
    public long contarMisDomicilios(String auth0Id) {
        logger.debug("Counting domicilios for user: {}", auth0Id);

        Long clienteId = getClienteIdByAuth0Id(auth0Id);
        return domicilioRepository.countByClienteId(clienteId);
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Obtiene el cliente por Auth0 ID (versión completa)
     */
    private Cliente getClienteByAuth0Id(String auth0Id) {
        return clienteRepository.findByUsuarioAuth0IdLight(auth0Id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado para el usuario autenticado"));
    }

    /**
     * Obtiene solo el ID del cliente por Auth0 ID (optimizado)
     */
    private Long getClienteIdByAuth0Id(String auth0Id) {
        return clienteRepository.findClienteIdByAuth0Id(auth0Id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado para el usuario autenticado"));
    }
}