package com.elbuensabor.services.impl;

import com.elbuensabor.dto.response.ClienteResponseDTO;
import com.elbuensabor.entities.Cliente;
import com.elbuensabor.entities.Rol;
import com.elbuensabor.exceptions.ResourceNotFoundException;
import com.elbuensabor.repository.IClienteRepository;
import com.elbuensabor.services.IRoleManagementService;
import com.elbuensabor.services.mapper.ClienteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoleManagementServiceImpl implements IRoleManagementService {

    private final IClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;

    @Autowired
    public RoleManagementServiceImpl(IClienteRepository clienteRepository, ClienteMapper clienteMapper) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
    }

    @Override
    public void changeUserRole(Long clienteId, Rol newRole) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        cliente.getUsuario().setRol(newRole);
        clienteRepository.save(cliente);
    }

    @Override
    public boolean isAdmin(String auth0Id) {
        return clienteRepository.findByUsuario_Auth0Id(auth0Id)
                .map(cliente -> cliente.getUsuario().getRol() == Rol.ADMIN)
                .orElse(false);
    }

    @Override
    public List<ClienteResponseDTO> getAllUsersWithRoles() {
        return clienteRepository.findAll()
                .stream()
                .map(clienteMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasRole(String auth0Id, Rol role) {
        return clienteRepository.findByUsuario_Auth0Id(auth0Id)
                .map(cliente -> cliente.getUsuario().getRol() == role)
                .orElse(false);
    }
}