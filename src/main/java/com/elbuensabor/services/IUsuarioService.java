package com.elbuensabor.services;

import com.elbuensabor.dto.response.UsuarioGridResponseDTO;

import java.util.List;

public interface IUsuarioService {
    List<UsuarioGridResponseDTO> obtenerUsuariosParaGrilla();
}
