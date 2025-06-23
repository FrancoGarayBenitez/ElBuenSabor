package com.elbuensabor.services.impl;

import com.elbuensabor.dto.request.ClienteRegisterDTO;
import com.elbuensabor.dto.response.ClienteResponseDTO;
import com.elbuensabor.dto.response.LoginResponseDTO;
import com.elbuensabor.entities.*;
import com.elbuensabor.exceptions.DuplicateResourceException;
import com.elbuensabor.exceptions.ResourceNotFoundException;
import com.elbuensabor.repository.IClienteRepository;
import com.elbuensabor.services.IAuth0Service;
import com.elbuensabor.services.mapper.ClienteMapper;
import com.elbuensabor.services.mapper.DomicilioMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class Auth0ServiceImpl implements IAuth0Service {

    private static final Logger logger = LoggerFactory.getLogger(Auth0ServiceImpl.class);
    private static final String NAMESPACE = "https://APIElBuenSabor";
    private static final String ROLES_CLAIM = NAMESPACE + "/roles";

    private final IClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;
    private final DomicilioMapper domicilioMapper;

    public Auth0ServiceImpl(IClienteRepository clienteRepository,
                            ClienteMapper clienteMapper,
                            DomicilioMapper domicilioMapper) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
        this.domicilioMapper = domicilioMapper;
    }

    @Override
    public LoginResponseDTO processAuth0User(Jwt jwt, Map<String, Object> userData) {
        logger.info("Processing Auth0 user login: {}", jwt.getSubject());

        String auth0Id = jwt.getSubject();
        UserData extractedData = extractUserData(jwt, userData);
        Rol userRole = extractRoleFromJwt(jwt);

        Cliente cliente = findOrCreateClienteFromAuth0(
                auth0Id,
                extractedData.email,
                extractedData.nombre,
                extractedData.apellido,
                userRole
        );

        return new LoginResponseDTO(
                jwt.getTokenValue(),
                cliente.getUsuario().getEmail(),
                cliente.getUsuario().getRol().name(),
                cliente.getUsuario().getIdUsuario(),
                cliente.getNombre(),
                cliente.getApellido()
        );
    }

    @Override
    public ClienteResponseDTO registerClienteFromAuth0(Jwt jwt, ClienteRegisterDTO registerDTO) {
        logger.info("Registering Auth0 user with additional data: {}", jwt.getSubject());

        String auth0Id = jwt.getSubject();
        String email = jwt.getClaimAsString("email");

        if (clienteRepository.findByUsuarioAuth0Id(auth0Id).isPresent()) {
            throw new DuplicateResourceException("El usuario ya está registrado");
        }

        if (email != null && !email.equals(registerDTO.getEmail())) {
            throw new IllegalArgumentException("El email del token no coincide con el email de registro");
        }

        Rol userRole = extractRoleFromJwt(jwt);
        Cliente cliente = createClienteFromRegisterDTO(registerDTO, auth0Id, userRole);
        Cliente savedCliente = clienteRepository.save(cliente);

        logger.info("Cliente registered successfully for Auth0 user: {}", auth0Id);
        return clienteMapper.toDTO(savedCliente);
    }

    @Override
    public ClienteResponseDTO completeUserProfile(Jwt jwt, ClienteRegisterDTO profileData) {
        logger.info("Completing profile for Auth0 user: {}", jwt.getSubject());

        String auth0Id = jwt.getSubject();
        Cliente cliente = clienteRepository.findByUsuarioAuth0Id(auth0Id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado. Debe hacer login primero."));

        updateClienteFromProfileData(cliente, profileData);
        Cliente updatedCliente = clienteRepository.save(cliente);

        logger.info("Profile completed successfully for Auth0 user: {}", auth0Id);
        return clienteMapper.toDTO(updatedCliente);
    }

    @Override
    public Map<String, Object> getCurrentUserProfile(Jwt jwt) {
        logger.debug("Getting profile for Auth0 user: {}", jwt.getSubject());

        Map<String, Object> profile = new HashMap<>();
        profile.put("authenticated", true);
        profile.put("auth_provider", "auth0");
        profile.put("sub", jwt.getSubject());
        profile.put("token_type", getTokenType(jwt));

        addClaimIfPresent(profile, jwt, "email");
        addClaimIfPresent(profile, jwt, "name");

        Object roles = jwt.getClaim(ROLES_CLAIM);
        if (roles != null) {
            profile.put("roles", roles);
        }

        return profile;
    }

    @Override
    public Map<String, Object> refreshUserRoles(Jwt jwt) {
        logger.info("Refreshing roles for user: {}", jwt.getSubject());

        String auth0Id = jwt.getSubject();
        Rol updatedRole = extractRoleFromJwt(jwt);

        Cliente cliente = clienteRepository.findByUsuarioAuth0Id(auth0Id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado en la base de datos"));

        Rol oldRole = cliente.getUsuario().getRol();

        if (!oldRole.equals(updatedRole)) {
            cliente.getUsuario().setRol(updatedRole);
            clienteRepository.save(cliente);

            logger.info("Updated role for user {} from {} to {}", auth0Id, oldRole, updatedRole);

            return Map.of(
                    "success", true,
                    "oldRole", oldRole.name(),
                    "newRole", updatedRole.name(),
                    "message", "Rol actualizado exitosamente"
            );
        } else {
            return Map.of(
                    "success", true,
                    "currentRole", updatedRole.name(),
                    "message", "El rol ya está actualizado"
            );
        }
    }

    @Override
    public Cliente findOrCreateClienteFromAuth0(String auth0Id, String email, String nombre, String apellido, Rol rol) {
        logger.debug("Finding or creating cliente for Auth0 ID: {}", auth0Id);

        // Buscar por Auth0 ID primero
        Optional<Cliente> existingByAuth0Id = clienteRepository.findByUsuarioAuth0Id(auth0Id);
        if (existingByAuth0Id.isPresent()) {
            Cliente cliente = existingByAuth0Id.get();
            updateClienteIfNeeded(cliente, email, nombre, apellido, rol);
            return clienteRepository.save(cliente);
        }

        // Buscar por email si no es temporal
        if (email != null && !isTemporaryEmail(email)) {
            Optional<Cliente> existingByEmail = clienteRepository.findByUsuarioEmail(email);
            if (existingByEmail.isPresent()) {
                Cliente cliente = existingByEmail.get();
                cliente.getUsuario().setAuth0Id(auth0Id);
                updateClienteIfNeeded(cliente, email, nombre, apellido, rol);
                return clienteRepository.save(cliente);
            }
        }

        // Crear nuevo cliente
        return createNewClienteFromAuth0(auth0Id, email, nombre, apellido, rol);
    }

    // === MÉTODOS PRIVADOS ===

    private UserData extractUserData(Jwt jwt, Map<String, Object> userData) {
        String email = extractString(userData, "email", jwt.getClaimAsString("email"));
        String nombre = extractString(userData, "given_name", jwt.getClaimAsString("given_name"));
        String apellido = extractString(userData, "family_name", jwt.getClaimAsString("family_name"));

        // Parsear nombre completo si es necesario
        if (isEmptyString(nombre) || isEmptyString(apellido)) {
            String fullName = extractString(userData, "name", jwt.getClaimAsString("name"));
            if (!isEmptyString(fullName)) {
                String[] nameParts = fullName.split(" ", 2);
                if (isEmptyString(nombre)) nombre = nameParts[0];
                if (isEmptyString(apellido)) apellido = nameParts.length > 1 ? nameParts[1] : "";
            }
        }

        // Generar email temporal si es necesario
        if (isEmptyString(email)) {
            email = "user-" + jwt.getSubject().replaceAll("[^a-zA-Z0-9]", "") + "@auth0.temp";
        }

        // Valores por defecto
        nombre = !isEmptyString(nombre) ? nombre : "Usuario";
        apellido = !isEmptyString(apellido) ? apellido : "Auth0";

        return new UserData(email, nombre, apellido);
    }

    private Rol extractRoleFromJwt(Jwt jwt) {
        Object rolesObj = jwt.getClaim(ROLES_CLAIM);
        logger.debug("Extracting role from JWT for user: {}", jwt.getSubject());

        if (rolesObj instanceof List) {
            List<?> roles = (List<?>) rolesObj;
            if (!roles.isEmpty()) {
                String role = roles.get(0).toString().toUpperCase();
                try {
                    Rol extractedRole = Rol.valueOf(role);
                    logger.debug("Extracted role {} for user {}", extractedRole, jwt.getSubject());
                    return extractedRole;
                } catch (IllegalArgumentException e) {
                    logger.warn("Unknown role '{}' for user {}, defaulting to CLIENTE", role, jwt.getSubject());
                }
            }
        }

        logger.debug("Using default CLIENTE role for user {}", jwt.getSubject());
        return Rol.CLIENTE;
    }

    private Cliente createClienteFromRegisterDTO(ClienteRegisterDTO registerDTO, String auth0Id, Rol userRole) {
        Usuario usuario = new Usuario();
        usuario.setAuth0Id(auth0Id);
        usuario.setEmail(registerDTO.getEmail());
        usuario.setPassword("");
        usuario.setRol(userRole);

        Cliente cliente = clienteMapper.toEntity(registerDTO);
        cliente.setUsuario(usuario);

        addDomicilioIfPresent(cliente, registerDTO);
        addImagenIfPresent(cliente, registerDTO);

        return cliente;
    }

    private void updateClienteFromProfileData(Cliente cliente, ClienteRegisterDTO profileData) {
        cliente.setNombre(profileData.getNombre());
        cliente.setApellido(profileData.getApellido());
        cliente.setTelefono(profileData.getTelefono());
        cliente.setFechaNacimiento(profileData.getFechaNacimiento());

        if (profileData.getEmail() != null && !profileData.getEmail().isEmpty()) {
            cliente.getUsuario().setEmail(profileData.getEmail());
        }

        updateDomicilioIfPresent(cliente, profileData);
        addImagenIfPresent(cliente, profileData);
    }

    private void updateClienteIfNeeded(Cliente cliente, String email, String nombre, String apellido, Rol rol) {
        // Actualizar rol si cambió
        if (!cliente.getUsuario().getRol().equals(rol)) {
            logger.info("Updating role for user {} from {} to {}",
                    cliente.getUsuario().getAuth0Id(), cliente.getUsuario().getRol(), rol);
            cliente.getUsuario().setRol(rol);
        }

        // Actualizar email si es válido y diferente
        if (email != null && !isTemporaryEmail(email) && !email.equals(cliente.getUsuario().getEmail())) {
            cliente.getUsuario().setEmail(email);
        }

        // Actualizar nombre/apellido si son mejores que los actuales
        if (shouldUpdateField(cliente.getNombre(), nombre)) {
            cliente.setNombre(nombre);
        }
        if (shouldUpdateField(cliente.getApellido(), apellido)) {
            cliente.setApellido(apellido);
        }
    }

    private Cliente createNewClienteFromAuth0(String auth0Id, String email, String nombre, String apellido, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setAuth0Id(auth0Id);
        usuario.setEmail(email);
        usuario.setPassword("");
        usuario.setRol(rol);

        Cliente cliente = new Cliente();
        cliente.setNombre(nombre);
        cliente.setApellido(apellido);
        cliente.setTelefono("");
        cliente.setFechaNacimiento(LocalDate.now().minusYears(18));
        cliente.setUsuario(usuario);
        cliente.setDomicilios(new ArrayList<>());
        cliente.setPedidos(new ArrayList<>());

        return clienteRepository.save(cliente);
    }

    // === MÉTODOS HELPER ===

    private void addDomicilioIfPresent(Cliente cliente, ClienteRegisterDTO registerDTO) {
        if (registerDTO.getDomicilio() != null) {
            Domicilio domicilio = domicilioMapper.toEntity(registerDTO.getDomicilio());
            domicilio.setCliente(cliente);
            cliente.getDomicilios().add(domicilio);
        }
    }

    private void updateDomicilioIfPresent(Cliente cliente, ClienteRegisterDTO profileData) {
        if (profileData.getDomicilio() != null) {
            cliente.getDomicilios().clear();
            Domicilio domicilio = domicilioMapper.toEntity(profileData.getDomicilio());
            domicilio.setCliente(cliente);
            cliente.getDomicilios().add(domicilio);
        }
    }

    private void addImagenIfPresent(Cliente cliente, ClienteRegisterDTO registerDTO) {
        if (registerDTO.getImagen() != null) {
            Imagen imagen = new Imagen();
            imagen.setDenominacion(registerDTO.getImagen().getDenominacion());
            imagen.setUrl(registerDTO.getImagen().getUrl());
            cliente.setImagen(imagen);
        }
    }

    private void addClaimIfPresent(Map<String, Object> profile, Jwt jwt, String claimName) {
        String claimValue = jwt.getClaimAsString(claimName);
        if (claimValue != null) {
            profile.put(claimName, claimValue);
        }
    }

    private String extractString(Map<String, Object> userData, String key, String fallback) {
        if (userData != null && userData.get(key) instanceof String) {
            return (String) userData.get(key);
        }
        return fallback;
    }

    private boolean isEmptyString(String str) {
        return str == null || str.trim().isEmpty();
    }

    private boolean shouldUpdateField(String currentValue, String newValue) {
        if (isEmptyString(newValue)) return false;
        if (isEmptyString(currentValue)) return true;
        return "Usuario".equals(currentValue) || "Auth0".equals(currentValue);
    }

    private boolean isTemporaryEmail(String email) {
        return email != null && email.contains("@auth0.temp");
    }

    private String getTokenType(Jwt jwt) {
        String grantType = jwt.getClaimAsString("gty");
        return "client-credentials".equals(grantType) ? "machine-to-machine" : "user";
    }

    // Clase interna para encapsular datos del usuario
    private static class UserData {
        final String email;
        final String nombre;
        final String apellido;

        UserData(String email, String nombre, String apellido) {
            this.email = email;
            this.nombre = nombre;
            this.apellido = apellido;
        }
    }
}