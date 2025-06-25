package com.elbuensabor.controllers;

import com.elbuensabor.dto.response.UsuarioGridResponseDTO;
import com.elbuensabor.services.IUsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final IUsuarioService usuarioService;

    @GetMapping("/grilla")
    public ResponseEntity<List<UsuarioGridResponseDTO>> obtenerUsuariosParaGrilla() {
        System.out.println("🎯 Entró al endpoint de usuarios");

        return ResponseEntity.ok(usuarioService.obtenerUsuariosParaGrilla());
    }
}