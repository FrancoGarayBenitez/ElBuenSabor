package com.elbuensabor.services.impl;

import com.elbuensabor.dto.request.ClienteRegisterDTO;
import com.elbuensabor.dto.response.ClienteResponseDTO;
import com.elbuensabor.entities.Cliente;
import com.elbuensabor.entities.Domicilio;
import com.elbuensabor.entities.Imagen;
import com.elbuensabor.entities.Usuario;
import com.elbuensabor.entities.Rol;
import com.elbuensabor.exceptions.DuplicateResourceException;
import com.elbuensabor.exceptions.ResourceNotFoundException;
import com.elbuensabor.repository.IClienteRepository;
import com.elbuensabor.services.IAuth0Service;
import com.elbuensabor.services.mapper.ClienteMapper;
import com.elbuensabor.services.mapper.DomicilioMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class Auth0ServiceImpl implements IAuth0Service {

    private final IClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;
    private final DomicilioMapper domicilioMapper;

    @Autowired
    public Auth0ServiceImpl(IClienteRepository clienteRepository,
                        ClienteMapper clienteMapper,
                        DomicilioMapper domicilioMapper) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
        this.domicilioMapper = domicilioMapper;
    }

    /**
     * Registra un cliente y lo sincroniza con Auth0
     */
    public ClienteResponseDTO registerClienteWithAuth0(ClienteRegisterDTO registerDTO, String auth0Id) {
        // Validar si el email ya existe
        if (clienteRepository.existsByUsuarioEmail(registerDTO.getEmail())) {
            throw new DuplicateResourceException("El email ya está registrado");
        }

        // Crear Usuario con Auth0 ID
        Usuario usuario = new Usuario();
        usuario.setAuth0Id(auth0Id);
        usuario.setEmail(registerDTO.getEmail());
        usuario.setPassword(""); // No se almacena password con Auth0
        usuario.setRol(Rol.CLIENTE);

        // Crear Cliente
        Cliente cliente = clienteMapper.toEntity(registerDTO);
        cliente.setUsuario(usuario);

        // Crear Domicilio
        if (registerDTO.getDomicilio() != null) {
            Domicilio domicilio = domicilioMapper.toEntity(registerDTO.getDomicilio());
            domicilio.setCliente(cliente);
            cliente.getDomicilios().add(domicilio);
        }

        // Manejar Imagen (si está presente)
        if (registerDTO.getImagen() != null) {
            Imagen imagen = new Imagen();
            imagen.setDenominacion(registerDTO.getImagen().getDenominacion());
            imagen.setUrl(registerDTO.getImagen().getUrl());
            imagen.setCliente(cliente);
            cliente.setImagen(imagen);
        }

        Cliente savedCliente = clienteRepository.save(cliente);
        return clienteMapper.toDTO(savedCliente);
    }

    /**
     * Obtiene o crea un cliente basado en el JWT de Auth0
     */
    public Cliente getOrCreateClienteFromJwt(Jwt jwt) {
        String auth0Id = jwt.getSubject(); // sub claim contiene el Auth0 ID
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");

        // Buscar cliente existente por Auth0 ID
        Cliente cliente = clienteRepository.findByUsuarioAuth0Id(auth0Id).orElse(null);

        if (cliente == null) {
            // Buscar por email como fallback
            cliente = clienteRepository.findByUsuarioEmail(email).orElse(null);

            if (cliente == null) {
                // Crear nuevo cliente básico
                cliente = createBasicClienteFromJwt(jwt, auth0Id, email, name);
            } else {
                // Actualizar Auth0 ID si el cliente existe pero no tiene Auth0 ID
                if (cliente.getUsuario().getAuth0Id() == null) {
                    cliente.getUsuario().setAuth0Id(auth0Id);
                    cliente = clienteRepository.save(cliente);
                }
            }
        }

        return cliente;
    }

    /**
     * Obtiene cliente por email
     */
    public Cliente getClienteByEmail(String email) {
        return clienteRepository.findByUsuarioEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
    }

    /**
     * Extrae email del JWT
     */
    public String extractEmailFromJwt(Jwt jwt) {
        return jwt.getClaimAsString("email");
    }

    /**
     * Extrae Auth0 ID del JWT
     */
    public String extractAuth0IdFromJwt(Jwt jwt) {
        return jwt.getSubject();
    }

    private Cliente createBasicClienteFromJwt(Jwt jwt, String auth0Id, String email, String name) {
        // Dividir nombre completo en nombre y apellido
        String[] nameParts = (name != null ? name : email).split(" ", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        // Crear Usuario
        Usuario usuario = new Usuario();
        usuario.setAuth0Id(auth0Id);
        usuario.setEmail(email);
        usuario.setPassword(""); // No password con Auth0
        usuario.setRol(Rol.CLIENTE);

        // Crear Cliente básico
        Cliente cliente = new Cliente();
        cliente.setNombre(firstName);
        cliente.setApellido(lastName);
        cliente.setTelefono(""); // Se puede completar después
        cliente.setFechaNacimiento(null); // Se puede completar después
        cliente.setUsuario(usuario);

        return clienteRepository.save(cliente);
    }
}