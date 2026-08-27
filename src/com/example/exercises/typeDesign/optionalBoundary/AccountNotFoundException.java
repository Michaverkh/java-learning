package com.example.exercises.typeDesign.optionalBoundary;

public class AccountNotFoundException extends RuntimeException {
    AccountNotFoundException() {
        super("Account ID should exist");
    }
}
