package com.example.wallet.clients;

import java.util.Optional;

public interface ClientRepository {
    Optional<Client> findById(ClientId id);

    void save(Client client);
}
