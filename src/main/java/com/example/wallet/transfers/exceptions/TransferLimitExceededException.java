package com.example.wallet.transfers.exceptions;

public class TransferLimitExceededException extends RuntimeException {
    public TransferLimitExceededException() {
        super("Сумма перевода превышает установленный лимит");
    }
}
