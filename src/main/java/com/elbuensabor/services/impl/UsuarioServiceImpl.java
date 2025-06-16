package com.elbuensabor.services.impl;

import com.elbuensabor.dto.response.UsuarioGridResponseDTO;
import com.elbuensabor.entities.Cliente;
import com.elbuensabor.entities.Usuario;
import com.elbuensabor.repository.IClienteRepository;
import com.elbuensabor.repository.IUsuarioRepository;
import com.elbuensabor.services.IUsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements IUsuarioService {

    private final IUsuarioRepository usuarioRepository;
    private final IClienteRepository clienteRepository;

    @Override
    public List<UsuarioGridResponseDTO> obtenerUsuariosParaGrilla() {
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
                    apellido
            ));
        }

        return resultado;
    }
}
