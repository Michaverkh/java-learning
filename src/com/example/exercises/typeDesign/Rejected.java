package com.example.exercises.typeDesign;

import java.util.Objects;

public record Rejected(RejectionReason reason) implements TransferResult {
    public Rejected {
        Objects.requireNonNull(reason);
    }
}
