package com.example.exercises.week_2.typeDesign.genericRepository;

import java.util.Optional;

public interface Repository<ID, ENTITY> {
    Optional<ENTITY> findById(ID id);

    void save(ENTITY entity);
}
