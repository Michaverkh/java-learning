package com.example.exercises.week_2.typeDesign;

import java.util.Objects;

public record Rejected(RejectionReason reason) implements TransferResult {
    public Rejected {
        Objects.requireNonNull(reason);
    }
}
