package com.example.wallet.transfers.exceptions;

public class SameAccountTransferException extends RuntimeException {
    public SameAccountTransferException() {
        super("Счета отправителя и получателя должны различаться");
    }
}
