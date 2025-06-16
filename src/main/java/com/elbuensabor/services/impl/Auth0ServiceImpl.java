package com.elbuensabor.services.impl;

<<<<<<< HEAD
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
=======
import com.elbuensabor.dto.response.LoginResponseDTO;
import com.elbuensabor.entities.Cliente;
import com.elbuensabor.entities.Rol;
import com.elbuensabor.entities.Usuario;
import com.elbuensabor.repository.IClienteRepository;
import com.elbuensabor.services.IAuth0Service;
import com.elbuensabor.services.mapper.ClienteMapper;
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
>>>>>>> ramaLucho

@Service
public class Auth0ServiceImpl implements IAuth0Service {

<<<<<<< HEAD
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

=======
    private static final Logger logger = LoggerFactory.getLogger(Auth0ServiceImpl.class);

    @Autowired
    private IClienteRepository clienteRepository;

    @Autowired
    private ClienteMapper clienteMapper;

    private static final String NAMESPACE = "https://APIElBuenSabor";
    private static final String ROLES_CLAIM = NAMESPACE + "/roles";

    @Override
    @Transactional
    public LoginResponseDTO processAuth0User(Jwt jwt, Map<String, Object> userData) {
        logger.info("=== AUTH0 JWT CLAIMS DEBUG ===");
        logger.info("All claims: {}", jwt.getClaims());

        String auth0Id = jwt.getSubject();

        // 🆕 USAR DATOS DEL FRONTEND SI ESTÁN DISPONIBLES
        String email = null;
        String name = null;
        String lastName = null;

        if (userData != null) {
            logger.info("=== USANDO DATOS DEL FRONTEND ===");
            email = (String) userData.get("email");
            name = (String) userData.get("given_name");
            lastName = (String) userData.get("family_name");
            String fullName = (String) userData.get("name");

            logger.info("Frontend data - email: {}, given_name: {}, family_name: {}, name: {}",
                    email, name, lastName, fullName);

            // Si no hay given_name/family_name, parsear nombre completo
            if ((name == null || name.isEmpty()) && fullName != null) {
                String[] nameParts = fullName.split(" ", 2);
                name = nameParts[0];
                lastName = nameParts.length > 1 ? nameParts[1] : "";
            }
        } else {
            logger.info("=== USANDO DATOS DEL JWT (FALLBACK) ===");
            email = jwt.getClaimAsString("email");
            name = jwt.getClaimAsString("given_name");
            lastName = jwt.getClaimAsString("family_name");
            String fullName = jwt.getClaimAsString("name");

            if ((name == null || name.isEmpty()) && fullName != null) {
                String[] nameParts = fullName.split(" ", 2);
                name = nameParts[0];
                lastName = nameParts.length > 1 ? nameParts[1] : "";
            }
        }

        logger.info("Final extracted values:");
        logger.info("- email: {}", email);
        logger.info("- name: {}", name);
        logger.info("- lastName: {}", lastName);

        // Validar email
        if (email == null || email.trim().isEmpty()) {
            if (auth0Id.contains("@clients")) {
                throw new IllegalArgumentException("Este tipo de token no es válido para login de usuarios. Use un token de usuario real.");
            }
            email = "user-" + auth0Id.replaceAll("[^a-zA-Z0-9]", "") + "@auth0.temp";
            logger.warn("Email no disponible, usando email temporal: {}", email);
        }

        // Valores por defecto
        name = name != null && !name.trim().isEmpty() ? name : "Usuario";
        lastName = lastName != null && !lastName.trim().isEmpty() ? lastName : "Auth0";

        // Extraer roles
        Rol userRole = extractRoleFromJwt(jwt);

        // Buscar o crear usuario
        Cliente cliente = findOrCreateClienteFromAuth0(auth0Id, email, name, lastName, userRole);

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
    public Cliente findOrCreateClienteFromAuth0(String auth0Id, String email, String nombre, String apellido, Rol rol) {
        logger.info("=== BÚSQUEDA DE USUARIO ===");
        logger.info("Buscando usuario con auth0Id: {}", auth0Id);
        logger.info("Email: {}", email);

        // Primero buscar por auth0Id
        try {
            Optional<Cliente> existingByAuth0Id = clienteRepository.findByUsuarioAuth0Id(auth0Id);
            if (existingByAuth0Id.isPresent()) {
                logger.info("✅ Usuario encontrado por auth0Id");
                Cliente cliente = existingByAuth0Id.get();

                // Actualizar datos si están vacíos o han cambiado
                updateClienteDataIfNeeded(cliente, email, nombre, apellido);

                return clienteRepository.save(cliente);
            }
            logger.info("❌ Usuario NO encontrado por auth0Id");
        } catch (Exception e) {
            logger.error("Error buscando por auth0Id: ", e);
        }

        // Luego buscar por email (solo si el email no es temporal)
        if (email != null && !email.contains("@auth0.temp")) {
            try {
                Optional<Cliente> existingByEmail = clienteRepository.findByUsuarioEmail(email);
                if (existingByEmail.isPresent()) {
                    logger.info("✅ Usuario encontrado por email, vinculando auth0Id");
                    Cliente cliente = existingByEmail.get();
                    cliente.getUsuario().setAuth0Id(auth0Id);

                    // Actualizar datos también
                    updateClienteDataIfNeeded(cliente, email, nombre, apellido);

                    return clienteRepository.save(cliente);
                }
                logger.info("❌ Usuario NO encontrado por email");
            } catch (Exception e) {
                logger.error("Error buscando por email: ", e);
            }
        }

        // Si llegamos aquí, crear nuevo cliente
        logger.info("🆕 Creando nuevo cliente");
        return createNewClienteFromAuth0(auth0Id, email, nombre, apellido, rol);
    }

    /**
     * Actualiza los datos del cliente si están vacíos o han cambiado
     */
    private void updateClienteDataIfNeeded(Cliente cliente, String email, String nombre, String apellido) {
        boolean updated = false;

        // Actualizar email si está vacío o es diferente
        if (email != null && !email.contains("@auth0.temp")) {
            if (cliente.getUsuario().getEmail() == null || cliente.getUsuario().getEmail().isEmpty()
                    || !cliente.getUsuario().getEmail().equals(email)) {
                logger.info("Actualizando email: {} -> {}", cliente.getUsuario().getEmail(), email);
                cliente.getUsuario().setEmail(email);
                updated = true;
            }
        }

        // Actualizar nombre si está vacío o es diferente
        if (nombre != null && !nombre.isEmpty()) {
            if (cliente.getNombre() == null || cliente.getNombre().isEmpty()
                    || cliente.getNombre().equals("Usuario")) {
                logger.info("Actualizando nombre: {} -> {}", cliente.getNombre(), nombre);
                cliente.setNombre(nombre);
                updated = true;
            }
        }

        // Actualizar apellido si está vacío o es diferente
        if (apellido != null && !apellido.isEmpty()) {
            if (cliente.getApellido() == null || cliente.getApellido().isEmpty()
                    || cliente.getApellido().equals("Auth0")) {
                logger.info("Actualizando apellido: {} -> {}", cliente.getApellido(), apellido);
                cliente.setApellido(apellido);
                updated = true;
            }
        }

        if (updated) {
            logger.info("✅ Datos del cliente actualizados");
        } else {
            logger.info("ℹ️ No se requieren actualizaciones");
        }
    }
    @Override
    public boolean isAuth0Token(String token) {
        // Los tokens de Auth0 son mucho más largos y tienen una estructura diferente
        // También puedes verificar la estructura del JWT
        return token != null && token.length() > 500; // Heurística simple
    }

    /**
     * Método privado para crear nuevo cliente desde Auth0
     */
    @Transactional
    private Cliente createNewClienteFromAuth0(String auth0Id, String email, String nombre, String apellido, Rol rol) {
>>>>>>> ramaLucho
        // Crear Usuario
        Usuario usuario = new Usuario();
        usuario.setAuth0Id(auth0Id);
        usuario.setEmail(email);
<<<<<<< HEAD
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
=======
        usuario.setPassword(""); // Los usuarios de Auth0 no tienen password local
        usuario.setRol(rol != null ? rol : Rol.CLIENTE);

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
     * Método privado para extraer rol del JWT
     */
    private Rol extractRoleFromJwt(Jwt jwt) {
        Object rolesObj = jwt.getClaim(ROLES_CLAIM);

        if (rolesObj instanceof List) {
            List<?> roles = (List<?>) rolesObj;
            if (!roles.isEmpty()) {
                String role = roles.get(0).toString().toUpperCase();
                try {
                    return Rol.valueOf(role);
                } catch (IllegalArgumentException e) {
                    // Si el rol no existe en nuestro enum, usar CLIENTE por defecto
                    return Rol.CLIENTE;
                }
            }
        }

        return Rol.CLIENTE;
    }
>>>>>>> ramaLucho
}