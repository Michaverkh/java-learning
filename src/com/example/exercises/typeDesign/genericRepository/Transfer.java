package com.example.exercises.typeDesign.genericRepository;

import com.example.exercises.typeDesign.TransferId;

import java.util.Objects;

public record Transfer(TransferId id) {
    public Transfer {
        Objects.requireNonNull(
                id,
                "id must not be null"
        );
    }

    public TransferId getId() {
        return id;
    }
}
