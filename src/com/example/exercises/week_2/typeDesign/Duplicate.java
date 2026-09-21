package com.example.exercises.week_2.typeDesign;

import java.util.Objects;

public record Duplicate(TransferId originalId) implements TransferResult {
    public Duplicate {
        Objects.requireNonNull(originalId);
    }
}
