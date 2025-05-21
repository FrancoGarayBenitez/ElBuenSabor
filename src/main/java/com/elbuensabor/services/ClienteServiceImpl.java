package com.elbuensabor.services;

import com.elbuensabor.entities.Cliente;
import com.elbuensabor.repository.ClienteRepository;
import com.elbuensabor.services.base.GenericServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ClienteServiceImpl extends GenericServiceImpl<Cliente, Long> implements ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteServiceImpl(ClienteRepository clienteRepository) {
        super(clienteRepository);
        this.clienteRepository = clienteRepository;
    }

    @Override
    public Optional<Cliente> findByEmail(String email) {
        return clienteRepository.findByEmail(email);
    }
}
