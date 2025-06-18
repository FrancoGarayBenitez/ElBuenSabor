package com.elbuensabor.services.impl;

import com.elbuensabor.dto.request.ClienteRegisterDTO;
import com.elbuensabor.dto.response.ClienteResponseDTO;
import com.elbuensabor.entities.*;
import com.elbuensabor.exceptions.DuplicateResourceException;
import com.elbuensabor.exceptions.ResourceNotFoundException;
import com.elbuensabor.repository.IClienteRepository;
import com.elbuensabor.services.IClienteService;
import com.elbuensabor.services.mapper.ClienteMapper;
import com.elbuensabor.services.mapper.DomicilioMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class ClienteServiceImpl extends GenericServiceImpl<Cliente, Long, ClienteResponseDTO, IClienteRepository, ClienteMapper>
        implements IClienteService {

    private final PasswordEncoder passwordEncoder;
    private final DomicilioMapper domicilioMapper;

    @Autowired
    public ClienteServiceImpl(IClienteRepository repository,
                              ClienteMapper mapper,
                              PasswordEncoder passwordEncoder,
                              DomicilioMapper domicilioMapper) {
        super(repository, mapper, Cliente.class, ClienteResponseDTO.class);
        this.passwordEncoder = passwordEncoder;
        this.domicilioMapper = domicilioMapper;
    }

    @Override
    public ClienteResponseDTO registerCliente(ClienteRegisterDTO registerDTO) {
        // Validaciones
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        if (repository.existsByUsuarioEmail(registerDTO.getEmail())) {
            throw new DuplicateResourceException("El email ya está registrado");
        }

        // Crear Usuario
        Usuario usuario = new Usuario();
        usuario.setEmail(registerDTO.getEmail());
        usuario.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        usuario.setRol(Rol.CLIENTE);

        // Crear Cliente
        Cliente cliente = mapper.toEntity(registerDTO);
        cliente.setUsuario(usuario);

        // Crear Domicilio
        Domicilio domicilio = domicilioMapper.toEntity(registerDTO.getDomicilio());
        domicilio.setCliente(cliente);
        cliente.getDomicilios().add(domicilio);

        // MANEJAR IMAGEN (si está presente)
        if (registerDTO.getImagen() != null) {
            Imagen imagen = new Imagen();
            imagen.setDenominacion(registerDTO.getImagen().getDenominacion());
            imagen.setUrl(registerDTO.getImagen().getUrl());
            imagen.setCliente(cliente);
            cliente.setImagen(imagen);
        }

        Cliente savedCliente = repository.save(cliente);

        // 🚨 ARREGLO MANUAL: Mapear con domicilios correctos
        return mapearClienteConDomicilios(savedCliente);
    }

    // 🚨 OVERRIDE del método findById para arreglar los domicilios también
    @Override
    public ClienteResponseDTO findById(Long id) {
        Cliente cliente = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));

        return mapearClienteConDomicilios(cliente);
    }

    // 🚨 OVERRIDE del método findAll para arreglar los domicilios también
    @Override
    public java.util.List<ClienteResponseDTO> findAll() {
        return repository.findAll().stream()
                .map(this::mapearClienteConDomicilios)
                .collect(Collectors.toList());
    }

    // ==================== MÉTODO AUXILIAR ====================
    private ClienteResponseDTO mapearClienteConDomicilios(Cliente cliente) {
        // Mapear cliente básico
        ClienteResponseDTO response = mapper.toDTO(cliente);

        // Sobrescribir los domicilios con el mapper correcto
        if (cliente.getDomicilios() != null && !cliente.getDomicilios().isEmpty()) {
            response.setDomicilios(
                    cliente.getDomicilios().stream()
                            .map(domicilioMapper::toResponseDTO)  // ← USAR toResponseDTO
                            .collect(Collectors.toList())
            );

            // 🐛 DEBUG - Remover en producción
            System.out.println("🏠 Domicilios mapeados: " + response.getDomicilios().size());
            response.getDomicilios().forEach(d ->
                    System.out.println("  - ID: " + d.getIdDomicilio() + ", Dirección: " + d.getCalle() + " " + d.getNumero())
            );
        }

        return response;
    }
}