package com.elbuensabor.services.impl;

import com.elbuensabor.dto.request.ClienteRegisterDTO;
import com.elbuensabor.dto.response.ClienteResponseDTO;
import com.elbuensabor.dto.response.LoginResponseDTO;
import com.elbuensabor.entities.*;
import com.elbuensabor.exceptions.DuplicateResourceException;
import com.elbuensabor.repository.IClienteRepository;
import com.elbuensabor.services.IAuth0Service;
import com.elbuensabor.services.mapper.ClienteMapper;
import com.elbuensabor.services.mapper.DomicilioMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación del servicio Auth0 para manejo de usuarios
 * Sincroniza usuarios entre Auth0 y la base de datos local
 */
@Service
public class Auth0ServiceImpl implements IAuth0Service {

    private static final Logger logger = LoggerFactory.getLogger(Auth0ServiceImpl.class);
    private static final String NAMESPACE = "https://APIElBuenSabor";
    private static final String ROLES_CLAIM = NAMESPACE + "/roles";

    @Autowired
    private IClienteRepository clienteRepository;

    @Autowired
    private ClienteMapper clienteMapper;

    @Autowired
    private DomicilioMapper domicilioMapper;

    @Override
    @Transactional
    public LoginResponseDTO processAuth0User(Jwt jwt, Map<String, Object> userData) {
        logger.info("Processing Auth0 user login: {}", jwt.getSubject());

        String auth0Id = jwt.getSubject();

        // Extraer datos del usuario (frontend tiene prioridad sobre JWT)
        UserData extractedData = extractUserData(jwt, userData);

        // Extraer rol del JWT
        Rol userRole = extractRoleFromJwt(jwt);

        // Buscar o crear cliente
        Cliente cliente = findOrCreateClienteFromAuth0(
                auth0Id,
                extractedData.email,
                extractedData.nombre,
                extractedData.apellido,
                userRole
        );

        // Construir respuesta de login
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
    @Transactional
    public ClienteResponseDTO registerClienteFromAuth0(Jwt jwt, ClienteRegisterDTO registerDTO) {
        logger.info("Registering Auth0 user with additional data: {}", jwt.getSubject());

        String auth0Id = jwt.getSubject();
        String email = jwt.getClaimAsString("email");

        // Validar que no exista ya un cliente con este auth0Id
        if (clienteRepository.findByUsuarioAuth0Id(auth0Id).isPresent()) {
            throw new DuplicateResourceException("El usuario ya está registrado");
        }

        // Validar email si está disponible
        if (email != null && !email.equals(registerDTO.getEmail())) {
            throw new IllegalArgumentException("El email del token no coincide con el email de registro");
        }

        // Extraer rol del JWT
        Rol userRole = extractRoleFromJwt(jwt);

        // Crear Usuario vinculado a Auth0
        Usuario usuario = new Usuario();
        usuario.setAuth0Id(auth0Id);
        usuario.setEmail(registerDTO.getEmail());
        usuario.setPassword(""); // Sin password local para usuarios de Auth0
        usuario.setRol(userRole);

        // Crear Cliente con datos del DTO
        Cliente cliente = clienteMapper.toEntity(registerDTO);
        cliente.setUsuario(usuario);

        // Crear Domicilio si está presente
        if (registerDTO.getDomicilio() != null) {
            Domicilio domicilio = domicilioMapper.toEntity(registerDTO.getDomicilio());
            domicilio.setCliente(cliente);
            cliente.getDomicilios().add(domicilio);
        }

        // Crear Imagen si está presente
        if (registerDTO.getImagen() != null) {
            Imagen imagen = new Imagen();
            imagen.setDenominacion(registerDTO.getImagen().getDenominacion());
            imagen.setUrl(registerDTO.getImagen().getUrl());
            cliente.setImagen(imagen);
        }

        // Guardar cliente
        Cliente savedCliente = clienteRepository.save(cliente);

        logger.info("Cliente registered successfully for Auth0 user: {}", auth0Id);

        return clienteMapper.toDTO(savedCliente);
    }

    @Override
    @Transactional
    public Cliente findOrCreateClienteFromAuth0(String auth0Id, String email, String nombre, String apellido, Rol rol) {
        logger.debug("Finding or creating cliente for Auth0 ID: {}", auth0Id);

        // Buscar por Auth0 ID primero
        Optional<Cliente> existingByAuth0Id = clienteRepository.findByUsuarioAuth0Id(auth0Id);
        if (existingByAuth0Id.isPresent()) {
            logger.debug("Found existing cliente by Auth0 ID");
            Cliente cliente = existingByAuth0Id.get();

            // CRÍTICO: Verificar y actualizar rol si cambió
            Rol currentRole = cliente.getUsuario().getRol();
            if (!currentRole.equals(rol)) {
                logger.info("⚠️ Updating role for user {} from {} to {}", auth0Id, currentRole, rol);
                cliente.getUsuario().setRol(rol);
            }

            // Actualizar otros datos también
            updateClienteDataIfNeeded(cliente, email, nombre, apellido);
            return clienteRepository.save(cliente);
        }

        // Buscar por email si el email no es temporal
        if (email != null && !isTemporaryEmail(email)) {
            Optional<Cliente> existingByEmail = clienteRepository.findByUsuarioEmail(email);
            if (existingByEmail.isPresent()) {
                logger.debug("Found existing cliente by email, linking Auth0 ID");
                Cliente cliente = existingByEmail.get();
                cliente.getUsuario().setAuth0Id(auth0Id);

                // CRÍTICO: También actualizar rol aquí
                Rol currentRole = cliente.getUsuario().getRol();
                if (!currentRole.equals(rol)) {
                    logger.info("⚠️ Updating role for user {} (by email) from {} to {}", auth0Id, currentRole, rol);
                    cliente.getUsuario().setRol(rol);
                }

                updateClienteDataIfNeeded(cliente, email, nombre, apellido);
                return clienteRepository.save(cliente);
            }
        }

        // Crear nuevo cliente
        logger.debug("Creating new cliente for Auth0 user");
        return createNewClienteFromAuth0(auth0Id, email, nombre, apellido, rol);
    }

    /**
     * Extrae datos del usuario priorizando datos del frontend sobre JWT
     */
    private UserData extractUserData(Jwt jwt, Map<String, Object> userData) {
        String email = null;
        String nombre = null;
        String apellido = null;

        // Priorizar datos del frontend
        if (userData != null) {
            email = (String) userData.get("email");
            nombre = (String) userData.get("given_name");
            apellido = (String) userData.get("family_name");

            // Parsear nombre completo si no hay given_name/family_name
            if ((nombre == null || nombre.isEmpty()) && userData.get("name") != null) {
                String[] nameParts = ((String) userData.get("name")).split(" ", 2);
                nombre = nameParts[0];
                apellido = nameParts.length > 1 ? nameParts[1] : "";
            }
        }

        // Fallback a datos del JWT
        if (email == null) email = jwt.getClaimAsString("email");
        if (nombre == null) nombre = jwt.getClaimAsString("given_name");
        if (apellido == null) apellido = jwt.getClaimAsString("family_name");

        // Parsear nombre completo del JWT si es necesario
        if ((nombre == null || nombre.isEmpty()) && jwt.getClaimAsString("name") != null) {
            String[] nameParts = jwt.getClaimAsString("name").split(" ", 2);
            nombre = nameParts[0];
            apellido = nameParts.length > 1 ? nameParts[1] : "";
        }

        // Generar email temporal si no hay email disponible
        if (email == null || email.trim().isEmpty()) {
            email = "user-" + jwt.getSubject().replaceAll("[^a-zA-Z0-9]", "") + "@auth0.temp";
        }

        // Valores por defecto
        nombre = (nombre != null && !nombre.trim().isEmpty()) ? nombre : "Usuario";
        apellido = (apellido != null && !apellido.trim().isEmpty()) ? apellido : "Auth0";

        return new UserData(email, nombre, apellido);
    }

    /**
     * Extrae el rol del JWT de Auth0
     * ACTUALIZADO: Con logs de debug
     */
    private Rol extractRoleFromJwt(Jwt jwt) {
        Object rolesObj = jwt.getClaim(ROLES_CLAIM);

        logger.debug("🔍 Extracting role from JWT for user: {}", jwt.getSubject());
        logger.debug("🔍 Raw roles claim: {}", rolesObj);

        if (rolesObj instanceof List) {
            List<?> roles = (List<?>) rolesObj;
            if (!roles.isEmpty()) {
                String role = roles.get(0).toString().toUpperCase();
                logger.debug("🔍 First role from list: {}", role);
                try {
                    Rol extractedRole = Rol.valueOf(role);
                    logger.info("✅ Extracted role {} for user {}", extractedRole, jwt.getSubject());
                    return extractedRole;
                } catch (IllegalArgumentException e) {
                    logger.warn("❌ Unknown role '{}' for user {}, defaulting to CLIENTE", role, jwt.getSubject());
                }
            } else {
                logger.warn("⚠️ Empty roles list for user {}, defaulting to CLIENTE", jwt.getSubject());
            }
        } else {
            logger.warn("⚠️ Roles claim is not a list for user {}: {} (type: {})",
                    jwt.getSubject(), rolesObj, rolesObj != null ? rolesObj.getClass().getSimpleName() : "null");
        }

        logger.info("🔄 Using default CLIENTE role for user {}", jwt.getSubject());
        return Rol.CLIENTE;
    }

    /**
     * Actualiza los datos del cliente si es necesario
     * ACTUALIZADO: También actualiza el rol si es diferente
     */
    private void updateClienteDataIfNeeded(Cliente cliente, String email, String nombre, String apellido) {
        boolean updated = false;

        // Actualizar email si es válido y diferente
        if (email != null && !isTemporaryEmail(email) && !email.equals(cliente.getUsuario().getEmail())) {
            cliente.getUsuario().setEmail(email);
            updated = true;
        }

        // Actualizar nombre si es mejor que el actual
        if (shouldUpdateField(cliente.getNombre(), nombre)) {
            cliente.setNombre(nombre);
            updated = true;
        }

        // Actualizar apellido si es mejor que el actual
        if (shouldUpdateField(cliente.getApellido(), apellido)) {
            cliente.setApellido(apellido);
            updated = true;
        }

        if (updated) {
            logger.debug("Updated cliente data for user: {}", cliente.getUsuario().getAuth0Id());
        }
    }

    /**
     * Determina si un campo debe ser actualizado
     */
    private boolean shouldUpdateField(String currentValue, String newValue) {
        if (newValue == null || newValue.trim().isEmpty()) return false;
        if (currentValue == null || currentValue.trim().isEmpty()) return true;

        // No actualizar si el valor actual no es genérico
        return "Usuario".equals(currentValue) || "Auth0".equals(currentValue);
    }

    /**
     * Verifica si un email es temporal
     */
    private boolean isTemporaryEmail(String email) {
        return email != null && email.contains("@auth0.temp");
    }

    /**
     * Crea un nuevo cliente desde Auth0
     */
    private Cliente createNewClienteFromAuth0(String auth0Id, String email, String nombre, String apellido, Rol rol) {
        // Crear Usuario
        Usuario usuario = new Usuario();
        usuario.setAuth0Id(auth0Id);
        usuario.setEmail(email);
        usuario.setPassword(""); // Sin password para usuarios de Auth0
        usuario.setRol(rol);

        // Crear Cliente
        Cliente cliente = new Cliente();
        cliente.setNombre(nombre);
        cliente.setApellido(apellido);
        cliente.setTelefono(""); // Se puede actualizar después
        cliente.setFechaNacimiento(LocalDate.now().minusYears(18)); // Fecha por defecto
        cliente.setUsuario(usuario);
        cliente.setDomicilios(new ArrayList<>());
        cliente.setPedidos(new ArrayList<>());

        return clienteRepository.save(cliente);
    }

    /**
     * Clase interna para encapsular datos del usuario
     */
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