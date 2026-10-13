package com.example.wallet.transfers.exceptions;

public class TransferNotFoundException extends RuntimeException {
    public TransferNotFoundException() {
        super("Перевод не найден");
    }
}
