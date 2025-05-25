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

@Service
public class ClienteServiceImpl extends GenericServiceImpl<Cliente, Long, ClienteResponseDTO, IClienteRepository, ClienteMapper>
        implements IClienteService {

    private final PasswordEncoder passwordEncoder;
    private final DomicilioMapper domicilioMapper;

    @Autowired
    public ClienteServiceImpl(IClienteRepository repository,
                              ClienteMapper mapper,
                              PasswordEncoder passwordEncoder,
                              DomicilioMapper domicilioMapper) { // Agregarlo al constructor
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
        return mapper.toDTO(savedCliente);
    }

}
