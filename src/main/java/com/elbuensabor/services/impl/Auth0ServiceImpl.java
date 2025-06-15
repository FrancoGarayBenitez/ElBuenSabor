package com.elbuensabor.services.impl;

import com.elbuensabor.dto.response.LoginResponseDTO;
import com.elbuensabor.entities.Cliente;
import com.elbuensabor.entities.Rol;
import com.elbuensabor.entities.Usuario;
import com.elbuensabor.repository.IClienteRepository;
import com.elbuensabor.services.IAuth0Service;
import com.elbuensabor.services.mapper.ClienteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class Auth0ServiceImpl implements IAuth0Service {

    @Autowired
    private IClienteRepository clienteRepository;

    @Autowired
    private ClienteMapper clienteMapper;

    private static final String NAMESPACE = "https://APIElBuenSabor";
    private static final String ROLES_CLAIM = NAMESPACE + "/roles";

    @Override
    @Transactional
    public LoginResponseDTO processAuth0User(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        String auth0Id = jwt.getSubject(); // 'sub' claim contiene el Auth0 ID
        String name = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");
        String fullName = jwt.getClaimAsString("name");

        // Si no hay given_name/family_name, intentar parsear el nombre completo
        if (name == null && fullName != null) {
            String[] nameParts = fullName.split(" ", 2);
            name = nameParts[0];
            lastName = nameParts.length > 1 ? nameParts[1] : "";
        }

        // Valores por defecto si no se encuentran
        name = name != null ? name : "Usuario";
        lastName = lastName != null ? lastName : "Auth0";

        // Extraer roles
        Rol userRole = extractRoleFromJwt(jwt);

        // Buscar o crear usuario
        Cliente cliente = findOrCreateClienteFromAuth0(auth0Id, email, name, lastName, userRole);

        return new LoginResponseDTO(
                jwt.getTokenValue(), // El token de Auth0
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
        // Primero buscar por auth0Id
        Optional<Cliente> existingByAuth0Id = clienteRepository.findByUsuarioAuth0Id(auth0Id);
        if (existingByAuth0Id.isPresent()) {
            return existingByAuth0Id.get();
        }

        // Luego buscar por email
        Optional<Cliente> existingByEmail = clienteRepository.findByUsuarioEmail(email);
        if (existingByEmail.isPresent()) {
            // Usuario existe pero no tiene auth0Id, vincular las cuentas
            Cliente cliente = existingByEmail.get();
            cliente.getUsuario().setAuth0Id(auth0Id);
            return clienteRepository.save(cliente);
        }

        // Crear nuevo cliente
        return createNewClienteFromAuth0(auth0Id, email, nombre, apellido, rol);
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
        // Crear Usuario
        Usuario usuario = new Usuario();
        usuario.setAuth0Id(auth0Id);
        usuario.setEmail(email);
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
}