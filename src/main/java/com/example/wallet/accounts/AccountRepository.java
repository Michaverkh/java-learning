package com.example.wallet.accounts;

import com.example.wallet.clients.ClientId;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    Optional<Account> findById(AccountId id);

    void save(Account account);

    List<Account> findAllByClientId(ClientId clientId);
}
