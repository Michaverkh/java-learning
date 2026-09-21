package com.example.exercises.week_2.typeDesign;

import java.util.Objects;

public record Succeeded(TransferId id) implements TransferResult {
    public Succeeded {
        Objects.requireNonNull(id);
    }
}
