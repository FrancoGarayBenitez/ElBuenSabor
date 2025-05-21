package com.elbuensabor.services.base;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public interface GenericService<T, ID extends Serializable> {
    T save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    void deleteById(ID id);
}

