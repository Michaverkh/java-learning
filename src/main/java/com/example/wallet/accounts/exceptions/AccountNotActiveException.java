package com.example.wallet.accounts.exceptions;

public class AccountNotActiveException extends RuntimeException {
    public AccountNotActiveException() {
        super("Оба счёта должны быть активны");
    }
}
