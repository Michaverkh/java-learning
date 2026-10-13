package com.example.wallet.clients;

import java.util.Objects;

public class ClientNotFoundException extends RuntimeException {
    public ClientNotFoundException(ClientId clientId) {
        super("Клиент не найден: " + Objects.requireNonNull(clientId, "clientId must not be null").value());
    }
}
