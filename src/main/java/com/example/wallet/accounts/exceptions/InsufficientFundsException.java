package com.example.wallet.accounts.exceptions;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException() {
        super("Недостаточно средств на счёте отправителя");
    }
}
