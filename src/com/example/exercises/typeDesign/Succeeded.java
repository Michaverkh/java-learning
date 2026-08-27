package com.example.exercises.typeDesign;

import java.util.Objects;

public record Succeeded(TransferId id) implements TransferResult {
    public Succeeded {
        Objects.requireNonNull(id);
    }
}
