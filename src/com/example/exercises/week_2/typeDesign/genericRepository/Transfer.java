package com.example.exercises.week_2.typeDesign.genericRepository;

import com.example.exercises.week_2.typeDesign.TransferId;

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
