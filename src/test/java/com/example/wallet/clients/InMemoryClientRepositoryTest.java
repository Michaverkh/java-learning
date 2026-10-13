package com.example.wallet.clients;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryClientRepositoryTest {
    private final ClientRepository repository = new InMemoryClientRepository();

    @Test
    void findsSavedClientAndReturnsEmptyForUnknownId() {
        Client client = client(new ClientId(UUID.randomUUID()), "external-1", "Иван");
        repository.save(client);

        assertEquals(client, repository.findById(new ClientId(client.id().value())).orElseThrow());
        assertTrue(repository.findById(new ClientId(UUID.randomUUID())).isEmpty());
    }

    @Test
    void replacesClientWithSameId() {
        ClientId id = new ClientId(UUID.randomUUID());
        repository.save(client(id, "external-1", "Иван"));
        Client updated = client(id, "external-1", "Иван Петров");
        repository.save(updated);

        assertEquals(updated, repository.findById(id).orElseThrow());
    }

    @Test
    void rejectsDuplicateExternalIdWithoutSavingClient() {
        Client original = client(new ClientId(UUID.randomUUID()), "external-1", "Иван");
        Client duplicate = client(new ClientId(UUID.randomUUID()), "external-1", "Анна");
        repository.save(original);

        assertThrows(IllegalArgumentException.class, () -> repository.save(duplicate));
        assertEquals(original, repository.findById(original.id()).orElseThrow());
        assertTrue(repository.findById(duplicate.id()).isEmpty());
    }

    private static Client client(ClientId id, String externalId, String name) {
        Instant now = Instant.parse("2026-10-06T09:00:00Z");
        return new Client(id, externalId, name, ClientStatus.ACTIVE, now, now);
    }
}
