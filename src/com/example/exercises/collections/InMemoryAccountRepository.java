package com.example.exercises.collections;

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

    // В этом случае поиск будет выполнен за O(accounts.values().lenght)
    public List<Account> findAllByClientId(ClientId id) {
        List<Account> result = new ArrayList<>();

        for (Account account : accounts.values()) {
            if (account.clientId().equals(id)) {
                result.add(account);
            }
        }

        return result;
    }

    // В этом случае поиск будет выполнен за O(k log k)
    public List<Account> findAllByClientIdWithHelpOfSet(ClientId clientId) {
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
