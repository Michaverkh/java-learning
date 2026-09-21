package com.example.exercises.week_3.lambdas;

@FunctionalInterface
public interface TransferValidator {
    void validate(TransferContext context);

    default TransferValidator andThen(TransferValidator next) {
        return context -> {
            this.validate(context);
            next.validate(context);
        };
    }
}