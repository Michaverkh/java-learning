package com.example.exercises.typeDesign;

import java.util.Objects;

public record Duplicate(TransferId originalId) implements TransferResult {
    public Duplicate {
        Objects.requireNonNull(originalId);
    }
}
