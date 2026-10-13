package com.example.wallet.transfers.exceptions;

/**
 * Внутренняя ошибка подготовки перевода; подробности не возвращаются клиенту.
 */
public class TransferCreationException extends RuntimeException {
    public TransferCreationException(String message) {
        super(message);
    }
}
