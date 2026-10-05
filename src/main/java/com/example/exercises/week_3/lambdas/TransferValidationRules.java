package com.example.exercises.week_3.lambdas;

public final class TransferValidationRules {

    public static TransferValidator differentAccounts = (context) -> {
        if (context.sourceAccount().getId().equals(context.targetAccount().getId())) {
            throw new RuntimeException("source account and target account should be different");
        }
    };

    public static TransferValidator activeAccounts = (context) -> {
        if (!context.sourceAccount().getIsActive() || !context.targetAccount().getIsActive()) {
            throw new RuntimeException("source account and target account should be active");
        }
    };

    public static void validateContext(TransferContext context) {
        TransferValidator finalValidator = differentAccounts.andThen(activeAccounts);
        finalValidator.validate(context);
    }
}
