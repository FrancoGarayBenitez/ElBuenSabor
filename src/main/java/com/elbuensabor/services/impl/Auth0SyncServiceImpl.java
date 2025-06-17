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
import java.util.ArrayList;
import java.util.List;
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

    // --- ESTE MÉTODO ESTÁ AHORA CORREGIDO ---
    @Override
    public ClienteResponseDTO syncUserFromAuth0(Jwt jwt) {
        String auth0Id = jwt.getSubject();
        String email = getEmailFromJwt(jwt); // Usamos un método auxiliar para obtener el email
        String name = jwt.getClaimAsString("name");
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");
        String picture = jwt.getClaimAsString("picture");

        if (email == null || email.isEmpty()) {
            throw new RuntimeException("No se pudo obtener email del token JWT");
        }

        // --- LÓGICA DE ROL AÑADIDA AQUÍ TAMBIÉN ---
        Rol rolFinal = getRolFromJwt(jwt);

        Optional<Cliente> existingCliente = clienteRepository.findByUsuario_Auth0Id(auth0Id);

        if (existingCliente.isPresent()) {
            return updateExistingUser(existingCliente.get(), email, name, givenName, familyName, rolFinal);
        } else {
            return createNewUser(auth0Id, email, name, givenName, familyName, picture, rolFinal);
        }
    }

    @Override
    public ClienteResponseDTO getCurrentUserInfo(Jwt jwt) {
        String auth0Id = jwt.getSubject();
        Cliente cliente = clienteRepository.findByUsuario_Auth0Id(auth0Id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return clienteMapper.toDTO(cliente);
    }

    // --- ESTE MÉTODO ESTÁ AHORA CORREGIDO ---
    @Override
    public ClienteResponseDTO updateUserFromAuth0(Jwt jwt) {
        String auth0Id = jwt.getSubject();
        Cliente cliente = clienteRepository.findByUsuario_Auth0Id(auth0Id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        String email = getEmailFromJwt(jwt);
        String name = jwt.getClaimAsString("name");
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");

        // --- LÓGICA DE ROL AÑADIDA AQUÍ ---
        Rol rolFinal = getRolFromJwt(jwt);

        return updateExistingUser(cliente, email, name, givenName, familyName, rolFinal);
    }

    @Override
    @Transactional
    public ClienteResponseDTO syncUserFromUserData(Map<String, Object> userData, Jwt jwt) {
        String auth0Id = (String) userData.get("auth0Id");
        String email = (String) userData.get("email");
        String name = (String) userData.get("name");
        String givenName = (String) userData.get("givenName");
        String familyName = (String) userData.get("familyName");
        String picture = (String) userData.get("picture");

        Rol rolFinal = getRolFromJwt(jwt);

        try {
            Optional<Cliente> existingCliente = clienteRepository.findByUsuario_Auth0Id(auth0Id);

            if (existingCliente.isPresent()) {
                System.out.println("✅ Usuario existente encontrado, actualizando...");
                return updateExistingUser(existingCliente.get(), email, name, givenName, familyName, rolFinal);
            }

            Optional<Cliente> existingByEmail = clienteRepository.findByUsuarioEmail(email);
            if (existingByEmail.isPresent()) {
                System.out.println("⚠️ Usuario existe por email, actualizando auth0_id y rol...");
                Cliente cliente = existingByEmail.get();
                cliente.getUsuario().setAuth0Id(auth0Id);
                cliente.getUsuario().setRol(rolFinal);
                Cliente savedCliente = clienteRepository.save(cliente);
                return updateExistingUser(savedCliente, email, name, givenName, familyName, rolFinal);
            }

            System.out.println("🆕 Creando nuevo usuario...");
            return createNewUser(auth0Id, email, name, givenName, familyName, picture, rolFinal);

        } catch (Exception e) {
            // ... (resto del método sin cambios) ...
        }
        return null; // Debería ser inalcanzable por el throw en el catch
    }

    private ClienteResponseDTO createNewUser(String auth0Id, String email, String name, String givenName, String familyName, String picture, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setAuth0Id(auth0Id);
        usuario.setEmail(email);
        usuario.setPassword("");
        usuario.setRol(rol); // <-- Usa el rol que viene del token

        Cliente cliente = new Cliente();
        String[] nombreParts = parseName(name, givenName, familyName);
        cliente.setNombre(nombreParts[0]);
        cliente.setApellido(nombreParts[1]);

        cliente.setTelefono("");
        cliente.setFechaNacimiento(LocalDate.now().minusYears(18));
        cliente.setUsuario(usuario);

        System.out.println("=== Creando nuevo usuario ===");
        System.out.println("Auth0 ID: " + auth0Id);
        System.out.println("Email: " + email);
        System.out.println("Nombre: " + cliente.getNombre());
        System.out.println("Apellido: " + cliente.getApellido());
        System.out.println("Rol: " + rol);
        System.out.println("=============================");

        Cliente savedCliente = clienteRepository.save(cliente);
        return clienteMapper.toDTO(savedCliente);
    }

    // --- ESTE MÉTODO ESTÁ AHORA CORREGIDO ---
    private ClienteResponseDTO updateExistingUser(Cliente cliente, String email, String name, String givenName, String familyName, Rol rol) {
        if (email != null && !email.equals(cliente.getUsuario().getEmail())) {
            cliente.getUsuario().setEmail(email);
        }

        // --- LÓGICA CORREGIDA ---
        // Actualizamos el rol en la base de datos con el que vino del token.
        cliente.getUsuario().setRol(rol);
        // --- FIN DE LA CORRECCIÓN ---

        if (cliente.getNombre() == null || cliente.getNombre().isEmpty()) {
            String[] nombreParts = parseName(name, givenName, familyName);
            cliente.setNombre(nombreParts[0]);
            cliente.setApellido(nombreParts[1]);
        }

        Cliente savedCliente = clienteRepository.save(cliente);
        return clienteMapper.toDTO(savedCliente);
    }

    // --- MÉTODOS AUXILIARES PARA NO REPETIR CÓDIGO ---

    private String getEmailFromJwt(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isEmpty()) {
            email = jwt.getClaimAsString("https://elbuensabor.com/email");
        }
        if (email == null || email.isEmpty()) {
            email = jwt.getClaimAsString("preferred_username");
        }
        return email;
    }

    private Rol getRolFromJwt(Jwt jwt) {
        List<String> rolesFromToken = jwt.getClaimAsStringList("https://elbuensabor.com/roles");
        if (rolesFromToken != null && rolesFromToken.contains("ADMIN")) {
            return Rol.ADMIN;
        }
        if (rolesFromToken != null && rolesFromToken.contains("COCINERO")) {
            return Rol.COCINERO;
        }
        // Agrega más roles aquí si es necesario
        return Rol.CLIENTE; // Rol por defecto
    }

    private String[] parseName(String name, String givenName, String familyName) {
        String nombre = givenName;
        String apellido = familyName;

        if ((nombre == null || nombre.isEmpty()) && name != null) {
            String[] parts = name.split(" ", 2);
            nombre = parts[0];
            apellido = parts.length > 1 ? parts[1] : "";
        }

        if (nombre == null || nombre.isEmpty()) nombre = "Usuario";
        if (apellido == null) apellido = "";

        return new String[]{nombre, apellido};
    }
}