package com.example.exercises.week_2.typeDesign;

import java.util.Objects;

public record Pending(TransferId id) implements TransferResult {
    public Pending {
        Objects.requireNonNull(id);
    }
}
