package com.elbuensabor.services.impl;

import com.elbuensabor.dto.response.UsuarioGridResponseDTO;
import com.elbuensabor.entities.Cliente;
import com.elbuensabor.entities.Rol;
import com.elbuensabor.entities.Usuario;
import com.elbuensabor.exceptions.ResourceNotFoundException;
import com.elbuensabor.repository.IClienteRepository;
import com.elbuensabor.repository.IUsuarioRepository;
import com.elbuensabor.services.IUsuarioService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del servicio de gestión de usuarios
 */
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements IUsuarioService {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioServiceImpl.class);

    private final IUsuarioRepository usuarioRepository;
    private final IClienteRepository clienteRepository;

    @Autowired(required = false)  // Optional injection
    private Auth0RoleSyncService auth0RoleSyncService;

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioGridResponseDTO> obtenerUsuariosParaGrilla() {
        logger.debug("Obteniendo todos los usuarios para grilla");

        List<Usuario> usuarios = usuarioRepository.findAll();
        List<UsuarioGridResponseDTO> resultado = new ArrayList<>();

        for (Usuario usuario : usuarios) {
            Optional<Cliente> clienteOpt = clienteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario());

            String nombre = clienteOpt.map(Cliente::getNombre).orElse("-");
            String apellido = clienteOpt.map(Cliente::getApellido).orElse("-");

            resultado.add(new UsuarioGridResponseDTO(
                    usuario.getIdUsuario(),
                    usuario.getEmail(),
                    usuario.getRol().toString(),
                    nombre,
                    apellido,
                    usuario.isActivo() // ← Usar el campo real de la BD
            ));
        }

        logger.debug("Retornando {} usuarios", resultado.size());
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioGridResponseDTO obtenerUsuarioPorId(Long idUsuario) {
        logger.debug("Obteniendo usuario por ID: {}", idUsuario);

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + idUsuario));

        Optional<Cliente> clienteOpt = clienteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario());

        String nombre = clienteOpt.map(Cliente::getNombre).orElse("-");
        String apellido = clienteOpt.map(Cliente::getApellido).orElse("-");

        return new UsuarioGridResponseDTO(
                usuario.getIdUsuario(),
                usuario.getEmail(),
                usuario.getRol().toString(),
                nombre,
                apellido,
                usuario.isActivo() // ← Usar el campo real de la BD
        );
    }

    @Override
    @Transactional
    public UsuarioGridResponseDTO cambiarRol(Long idUsuario, String nuevoRol) {
        logger.info("Cambiando rol de usuario {} a {}", idUsuario, nuevoRol);

        // Buscar usuario
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + idUsuario));

        // Validar nuevo rol
        Rol rolEnum;
        try {
            rolEnum = Rol.valueOf(nuevoRol.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol inválido: " + nuevoRol);
        }

        String rolAnterior = usuario.getRol().toString();

        // Actualizar rol en base de datos
        usuario.setRol(rolEnum);
        usuarioRepository.save(usuario);

        logger.info("✅ Rol actualizado en BD para usuario {}: {} -> {}", idUsuario, rolAnterior, nuevoRol);

        // Sincronizar con Auth0 si el servicio está disponible
        if (auth0RoleSyncService != null && usuario.getAuth0Id() != null) {
            try {
                logger.info("🔄 Sincronizando rol con Auth0 para usuario: {}", usuario.getAuth0Id());
                auth0RoleSyncService.syncUserRole(usuario.getAuth0Id(), nuevoRol);
                logger.info("✅ Rol sincronizado exitosamente con Auth0");
            } catch (Exception e) {
                logger.warn("⚠️ No se pudo sincronizar rol con Auth0: {}", e.getMessage());
                // No fallar la operación si Auth0 falla
            }
        } else {
            logger.warn("⚠️ Auth0RoleSyncService no disponible o usuario sin Auth0 ID");
        }

        // Retornar usuario actualizado
        return obtenerUsuarioPorId(idUsuario);
    }

    @Override
    @Transactional
    public UsuarioGridResponseDTO cambiarEstado(Long idUsuario, boolean activo) {
        logger.info("{} usuario {}", activo ? "Activando" : "Desactivando", idUsuario);

        // Buscar usuario
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + idUsuario));

        // Actualizar estado
        usuario.setActivo(activo);
        usuarioRepository.save(usuario);

        logger.info("✅ Estado {} para usuario {} ({})",
                activo ? "ACTIVO" : "INACTIVO",
                idUsuario,
                usuario.getEmail());

        // TODO: Opcionalmente, también desactivar en Auth0
        // if (auth0RoleSyncService != null && usuario.getAuth0Id() != null) {
        //     auth0RoleSyncService.blockUser(usuario.getAuth0Id(), !activo);
        // }

        // Retornar usuario actualizado
        return obtenerUsuarioPorId(idUsuario);
    }

    @Override
    @Transactional(readOnly = true)
    public Long obtenerIdUsuarioPorAuth0Id(String auth0Id) {
        logger.debug("Obteniendo ID de usuario por Auth0 ID: {}", auth0Id);

        if (auth0Id == null || auth0Id.trim().isEmpty()) {
            throw new IllegalArgumentException("Auth0 ID no puede ser null o vacío");
        }

        Usuario usuario = usuarioRepository.findByAuth0Id(auth0Id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado para Auth0 ID: " + auth0Id));

        return usuario.getIdUsuario();
    }

    @Override
    @Transactional(readOnly = true)
    public long contarAdministradoresActivos() {
        logger.debug("Contando administradores activos");

        // TODO: Si tienes campo 'activo', usar: usuarioRepository.countByRolAndActivo(Rol.ADMIN, true)
        long count = usuarioRepository.countByRol(Rol.ADMIN);

        logger.debug("Administradores activos encontrados: {}", count);
        return count;
    }
}