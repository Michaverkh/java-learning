package com.example.exercises.typeDesign;

public sealed interface TransferResult
        permits Succeeded, Rejected, Duplicate, Pending {
}

