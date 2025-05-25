package com.elbuensabor.services;

import com.elbuensabor.dto.request.LoginRequestDTO;
import com.elbuensabor.dto.response.LoginResponseDTO;

public interface IAuthService {
    LoginResponseDTO authenticate(LoginRequestDTO loginRequest);
    boolean validateToken(String token);
    String extractEmailFromToken(String token);
}
