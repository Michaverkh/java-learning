package com.example.exercises.week_2.typeDesign;

public sealed interface TransferResult
        permits Succeeded, Rejected, Duplicate, Pending {
}

