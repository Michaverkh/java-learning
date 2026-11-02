package com.example.wallet.clients;

import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
public class InMemoryClientRepository implements ClientRepository {
    private final Map<ClientId, Client> clients = new HashMap<>();

    @Override
    public synchronized Optional<Client> findById(ClientId id) {
        Objects.requireNonNull(id, "id must not be null");
        return Optional.ofNullable(clients.get(id));
    }

    @Override
    public synchronized void save(Client client) {
        Objects.requireNonNull(client, "client must not be null");

        boolean externalIdTaken = clients.values().stream()
                .anyMatch(existing -> existing.externalId().equals(client.externalId())
                        && !existing.id().equals(client.id()));
        if (externalIdTaken) {
            throw new IllegalArgumentException("client externalId must be unique");
        }

        clients.put(client.id(), client);
    }
}
