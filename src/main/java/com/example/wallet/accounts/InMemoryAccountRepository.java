package com.example.wallet.accounts;

import com.example.wallet.clients.ClientId;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public final class InMemoryAccountRepository implements AccountRepository {
    private final Map<AccountId, Account> accounts = new HashMap<>();
    private final Map<ClientId, Set<AccountId>> accountIdsByClient = new HashMap<>();

    @Override
    public Optional<Account> findById(AccountId id) {
        return Optional.ofNullable(accounts.get(id));
    }

    @Override
    public void save(Account account) {
        Objects.requireNonNull(account, "account must not be null");
        accounts.put(account.id(), account);
        accountIdsByClient.computeIfAbsent(account.clientId(), ignored -> new HashSet<>()).add(account.id());
    }

    @Override
    public List<Account> findAllByClientId(ClientId clientId) {
        Set<AccountId> accountIds =
                accountIdsByClient.getOrDefault(clientId, Set.of());

        List<Account> result = new ArrayList<>(accountIds.size());

        for (AccountId accountId : accountIds) {
            Account account = accounts.get(accountId);

            if (account != null) {
                result.add(account);
            }
        }

        result.sort(Comparator.comparing(account -> account.id().getValue()));
        return List.copyOf(result);
    }
}
