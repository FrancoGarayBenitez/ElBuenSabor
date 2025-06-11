package com.elbuensabor.services;

import com.elbuensabor.dto.request.ClienteRegisterDTO;
import com.elbuensabor.dto.response.ClienteResponseDTO;
import com.elbuensabor.entities.Cliente;

public interface IClienteService extends IGenericService<Cliente, Long, ClienteResponseDTO> {
    ClienteResponseDTO registerCliente(ClienteRegisterDTO registerDTO);

    ClienteResponseDTO findByEmail(String email);

}