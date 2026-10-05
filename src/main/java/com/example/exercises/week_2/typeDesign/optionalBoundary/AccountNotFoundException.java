package com.example.exercises.week_2.typeDesign.optionalBoundary;

public class AccountNotFoundException extends RuntimeException {
    AccountNotFoundException() {
        super("Account ID should exist");
    }
}
