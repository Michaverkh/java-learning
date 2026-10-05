package com.example.exercises.week_3.collections;

import java.util.*;

public final class InMemoryAccountRepository {
    private final Map<AccountId, Account> accounts = new HashMap<>();
    private final Map<ClientId, Set<AccountId>> accountIdsByClient = new HashMap<>();

    public Optional<Account> findById(AccountId id) {
        return Optional.ofNullable(accounts.get(id));
    }

    public void save(Account account) {
        accounts.put(account.id(), account);
        accountIdsByClient.computeIfAbsent(account.clientId(), ignored -> new HashSet<>()).add(account.id());
    }

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

        result.sort(Comparator.comparing(account -> account.id().value()));
        return List.copyOf(result);
    }
}
