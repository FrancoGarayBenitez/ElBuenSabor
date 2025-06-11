package com.elbuensabor.services.impl;

import com.elbuensabor.config.JwtUtil;
import com.elbuensabor.dto.request.LoginRequestDTO;
import com.elbuensabor.dto.response.LoginResponseDTO;
import com.elbuensabor.entities.Cliente;
import com.elbuensabor.exceptions.ResourceNotFoundException;
import com.elbuensabor.repository.IClienteRepository;
import com.elbuensabor.services.IAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements IAuthService {

    @Autowired
    private IClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public LoginResponseDTO authenticate(LoginRequestDTO loginRequest) {
        try {
            // Buscar cliente por email
            Cliente cliente = clienteRepository.findByUsuarioEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

            // Verificar contraseña
            if (!passwordEncoder.matches(loginRequest.getPassword(), cliente.getUsuario().getPassword())) {
                throw new BadCredentialsException("Credenciales inválidas");
            }

            // Generar token JWT
            String token = jwtUtil.generateToken(
                    cliente.getUsuario().getEmail(),
                    cliente.getUsuario().getRol().name(),
                    cliente.getUsuario().getIdUsuario()
            );

            // Retornar respuesta exitosa
            return new LoginResponseDTO(
                    token,
                    cliente.getUsuario().getEmail(),
                    cliente.getUsuario().getRol().name(),
                    cliente.getUsuario().getIdUsuario(),
                    cliente.getNombre(),
                    cliente.getApellido()
            );

        } catch (ResourceNotFoundException | BadCredentialsException e) {
            throw new BadCredentialsException("Email o contraseña incorrectos");
        }
    }

    @Override
    public boolean validateToken(String token) {
        try {
            // Verificar que el token no sea nulo o vacío
            if (token == null || token.trim().isEmpty()) {
                return false;
            }

            // Verificar que el token no esté expirado
            if (jwtUtil.isTokenExpired(token)) {
                return false;
            }

            // Intentar extraer el email para verificar que el token es válido
            String email = jwtUtil.extractUsername(token);

            // Verificar que el email no sea nulo
            if (email == null || email.trim().isEmpty()) {
                return false;
            }

            // Opcional: Verificar que el usuario aún existe en la base de datos
            boolean userExists = clienteRepository.existsByUsuarioEmail(email);

            return userExists;

        } catch (Exception e) {
            // Log del error para debugging
            System.err.println("Error validating token: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String extractEmailFromToken(String token) {
        try {
            return jwtUtil.extractUsername(token);
        } catch (Exception e) {
            System.err.println("Error extracting email from token: " + e.getMessage());
            return null;
        }
    }
}
