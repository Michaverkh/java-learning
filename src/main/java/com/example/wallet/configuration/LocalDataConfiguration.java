package com.example.wallet.configuration;

import com.example.wallet.clients.Client;
import com.example.wallet.clients.ClientId;
import com.example.wallet.clients.ClientRepository;
import com.example.wallet.clients.ClientStatus;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Configuration
@Profile("local")
public class LocalDataConfiguration {
    @Bean
    public ApplicationRunner createTrainingClient(ClientRepository clientRepository, Clock clock) {
        return args -> {
            ClientId clientId = new ClientId(UUID.fromString("11111111-1111-4111-8111-111111111111"));
            if (clientRepository.findById(clientId).isEmpty()) {
                Instant now = clock.instant();
                clientRepository.save(new Client(
                        clientId,
                        "training-client-001",
                        "Иван Петров",
                        ClientStatus.ACTIVE,
                        now,
                        now
                ));
            }
        };
    }
}
