package com.example.wallet.transfers.exceptions;

public class TransferCurrencyMismatchException extends RuntimeException {
    public TransferCurrencyMismatchException() {
        super("Валюта перевода и обоих счетов должна быть RUB");
    }
}
