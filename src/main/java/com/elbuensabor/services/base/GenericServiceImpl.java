package com.elbuensabor.services.base;

import com.elbuensabor.repository.base.GenericRepository;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public class GenericServiceImpl<T, ID extends Serializable> implements GenericService<T, ID> {

    protected final GenericRepository<T, ID> repository;

    public GenericServiceImpl(GenericRepository<T, ID> repository) {
        this.repository = repository;
    }

    @Override
    public T save(T entity) {
        return repository.save(entity);
    }

    @Override
    public Optional<T> findById(ID id) {
        return repository.findById(id);
    }

    @Override
    public List<T> findAll() {
        return repository.findAll();
    }

    @Override
    public void deleteById(ID id) {
        repository.deleteById(id);
    }
}

