package com.elbuensabor.services.impl;

import com.elbuensabor.dto.response.ClienteResponseDTO;
import com.elbuensabor.entities.Cliente;
import com.elbuensabor.entities.Rol;
import com.elbuensabor.entities.Usuario;
import com.elbuensabor.exceptions.ResourceNotFoundException;
import com.elbuensabor.repository.IClienteRepository;
import com.elbuensabor.services.IAuth0SyncService;
import com.elbuensabor.services.mapper.ClienteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class Auth0SyncServiceImpl implements IAuth0SyncService {

    private final IClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;

    @Autowired
    public Auth0SyncServiceImpl(IClienteRepository clienteRepository, ClienteMapper clienteMapper) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
    }

    @Override
    public ClienteResponseDTO syncUserFromAuth0(Jwt jwt) {
        String auth0Id = jwt.getSubject();

        // Para tokens de Auth0, el email puede estar en diferentes claims
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isEmpty()) {
            // Fallback: buscar en otros claims posibles
            email = jwt.getClaimAsString("https://elbuensabor.com/email");
            if (email == null || email.isEmpty()) {
                email = jwt.getClaimAsString("preferred_username");
            }
        }

        String name = jwt.getClaimAsString("name");
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");
        String picture = jwt.getClaimAsString("picture");

        // Log para debugging
        System.out.println("=== JWT Claims Debug ===");
        System.out.println("Subject (auth0Id): " + auth0Id);
        System.out.println("Email: " + email);
        System.out.println("Name: " + name);
        System.out.println("Given Name: " + givenName);
        System.out.println("Family Name: " + familyName);
        System.out.println("Picture: " + picture);
        System.out.println("All claims: " + jwt.getClaims());
        System.out.println("========================");

        // Verificar que tenemos los datos mínimos
        if (email == null || email.isEmpty()) {
            throw new RuntimeException("No se pudo obtener email del token JWT");
        }

        // Buscar si el usuario ya existe por auth0_id
        Optional<Cliente> existingCliente = clienteRepository.findByUsuario_Auth0Id(auth0Id);

        if (existingCliente.isPresent()) {
            // Usuario existente, actualizar información si es necesario
            return updateExistingUser(existingCliente.get(), email, name, givenName, familyName);
        } else {
            // Nuevo usuario, crear registro con rol CLIENTE por defecto
            return createNewUser(auth0Id, email, name, givenName, familyName, picture);
        }
    }

    @Override
    public ClienteResponseDTO getCurrentUserInfo(Jwt jwt) {
        String auth0Id = jwt.getSubject();
        Cliente cliente = clienteRepository.findByUsuario_Auth0Id(auth0Id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return clienteMapper.toDTO(cliente);
    }

    @Override
    public ClienteResponseDTO updateUserFromAuth0(Jwt jwt) {
        String auth0Id = jwt.getSubject();
        Cliente cliente = clienteRepository.findByUsuario_Auth0Id(auth0Id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");

        return updateExistingUser(cliente, email, name, givenName, familyName);
    }

    @Override
    @Transactional
    public ClienteResponseDTO syncUserFromUserData(Map<String, Object> userData) {
        String auth0Id = (String) userData.get("auth0Id");
        String email = (String) userData.get("email");
        String name = (String) userData.get("name");
        String givenName = (String) userData.get("givenName");
        String familyName = (String) userData.get("familyName");
        String picture = (String) userData.get("picture");

        System.out.println("=== User Data Direct Debug ===");
        System.out.println("Auth0 ID: " + auth0Id);
        System.out.println("Email: " + email);
        System.out.println("Name: " + name);
        System.out.println("Given Name: " + givenName);
        System.out.println("Family Name: " + familyName);
        System.out.println("Picture: " + picture);
        System.out.println("===============================");

        // Verificar que tenemos los datos mínimos
        if (email == null || email.isEmpty()) {
            throw new RuntimeException("No se pudo obtener email de los datos del usuario");
        }
        if (auth0Id == null || auth0Id.isEmpty()) {
            throw new RuntimeException("No se pudo obtener auth0Id de los datos del usuario");
        }

        try {
            // Buscar si el usuario ya existe por auth0_id (con lock para evitar concurrencia)
            Optional<Cliente> existingCliente = clienteRepository.findByUsuario_Auth0Id(auth0Id);

            if (existingCliente.isPresent()) {
                System.out.println("✅ Usuario existente encontrado, actualizando...");
                return updateExistingUser(existingCliente.get(), email, name, givenName, familyName);
            }

            // Verificar también por email por si acaso
            Optional<Cliente> existingByEmail = clienteRepository.findByUsuarioEmail(email);
            if (existingByEmail.isPresent()) {
                System.out.println("⚠️ Usuario existe por email, actualizando auth0_id...");
                Cliente cliente = existingByEmail.get();
                cliente.getUsuario().setAuth0Id(auth0Id);
                Cliente savedCliente = clienteRepository.save(cliente);
                return updateExistingUser(savedCliente, email, name, givenName, familyName);
            }

            System.out.println("🆕 Creando nuevo usuario...");
            return createNewUser(auth0Id, email, name, givenName, familyName, picture);

        } catch (Exception e) {
            // Si es error de duplicado, intentar buscar el usuario recién creado
            if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                System.out.println("⚠️ Error de duplicado detectado, buscando usuario existente...");

                // Esperar un poco y buscar de nuevo
                try {
                    Thread.sleep(100);
                    Optional<Cliente> existingCliente = clienteRepository.findByUsuario_Auth0Id(auth0Id);
                    if (existingCliente.isPresent()) {
                        System.out.println("✅ Usuario encontrado después del error de duplicado");
                        return clienteMapper.toDTO(existingCliente.get());
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }

            System.out.println("❌ Error en syncUserFromUserData: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error sincronizando usuario: " + e.getMessage(), e);
        }
    }

    private ClienteResponseDTO createNewUser(String auth0Id, String email, String name, String givenName, String familyName, String picture) {
        // Crear Usuario con rol CLIENTE por defecto
        Usuario usuario = new Usuario();
        usuario.setAuth0Id(auth0Id);
        usuario.setEmail(email);
        usuario.setPassword(""); // No necesitamos password con Auth0
        usuario.setRol(Rol.CLIENTE); // SIEMPRE CLIENTE por defecto

        // Crear Cliente
        Cliente cliente = new Cliente();

        // Determinar nombre y apellido
        String nombre = givenName;
        String apellido = familyName;

        // Si no tenemos given_name y family_name, parsear el name
        if ((nombre == null || nombre.isEmpty()) && name != null) {
            String[] nombreParts = name.split(" ", 2);
            nombre = nombreParts[0];
            apellido = nombreParts.length > 1 ? nombreParts[1] : "";
        }

        // Valores por defecto si aún están vacíos
        if (nombre == null || nombre.isEmpty()) {
            nombre = "Usuario";
        }
        if (apellido == null) {
            apellido = "";
        }

        cliente.setNombre(nombre);
        cliente.setApellido(apellido);
        cliente.setTelefono(""); // Se completará después
        cliente.setFechaNacimiento(LocalDate.now().minusYears(18)); // Valor por defecto
        cliente.setUsuario(usuario);

        // Si hay imagen de perfil de Auth0, crear entidad Imagen
        if (picture != null && !picture.isEmpty()) {
            // Aquí podrías crear la entidad Imagen si quieres guardar la foto de perfil
            // Por ahora lo dejamos null
        }

        System.out.println("=== Creando nuevo usuario ===");
        System.out.println("Auth0 ID: " + auth0Id);
        System.out.println("Email: " + email);
        System.out.println("Nombre: " + nombre);
        System.out.println("Apellido: " + apellido);
        System.out.println("Rol: " + Rol.CLIENTE);
        System.out.println("=============================");

        try {
            Cliente savedCliente = clienteRepository.save(cliente);
            System.out.println("✅ Usuario creado exitosamente con ID: " + savedCliente.getIdCliente());
            return clienteMapper.toDTO(savedCliente);
        } catch (Exception e) {
            System.out.println("❌ Error guardando cliente: " + e.getMessage());
            throw e;
        }
    }

    private ClienteResponseDTO updateExistingUser(Cliente cliente, String email, String name, String givenName, String familyName) {
        // Actualizar email si cambió
        if (email != null && !email.equals(cliente.getUsuario().getEmail())) {
            cliente.getUsuario().setEmail(email);
        }

        // NO actualizar rol - se mantiene el que tiene asignado en nuestra BD

        // Actualizar nombre si es necesario (solo si los campos están vacíos)
        if (givenName != null && !givenName.isEmpty() && cliente.getNombre().isEmpty()) {
            cliente.setNombre(givenName);
        }
        if (familyName != null && !familyName.isEmpty() && cliente.getApellido().isEmpty()) {
            cliente.setApellido(familyName);
        }

        // Si no tenemos given_name/family_name pero sí name, usar como fallback
        if (name != null && (cliente.getNombre().isEmpty() || cliente.getApellido().isEmpty())) {
            String[] nombreParts = name.split(" ", 2);
            if (cliente.getNombre().isEmpty()) {
                cliente.setNombre(nombreParts[0]);
            }
            if (cliente.getApellido().isEmpty() && nombreParts.length > 1) {
                cliente.setApellido(nombreParts[1]);
            }
        }

        Cliente savedCliente = clienteRepository.save(cliente);
        return clienteMapper.toDTO(savedCliente);
    }
}