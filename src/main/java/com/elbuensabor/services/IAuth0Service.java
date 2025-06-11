package com.elbuensabor.services;

import com.elbuensabor.dto.request.ClienteRegisterDTO;
import com.elbuensabor.dto.response.ClienteResponseDTO;
import com.elbuensabor.entities.Cliente;
import org.springframework.security.oauth2.jwt.Jwt;

public interface IAuth0Service {

    ClienteResponseDTO registerClienteWithAuth0(ClienteRegisterDTO registerDTO, String auth0Id);

    Cliente getOrCreateClienteFromJwt(Jwt jwt);

    Cliente getClienteByEmail(String email);

    String extractEmailFromJwt(Jwt jwt);

    String extractAuth0IdFromJwt(Jwt jwt);
}