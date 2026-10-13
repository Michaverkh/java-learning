package com.example.wallet.accounts;

import com.example.wallet.accounts.exceptions.AccountNotFoundException;
import com.example.wallet.clients.ClientId;
import com.example.wallet.clients.ClientNotFoundException;
import com.example.wallet.clients.ClientRepository;
import com.example.wallet.ledger.LedgerEntriesQuery;
import com.example.wallet.ledger.LedgerEntry;
import com.example.wallet.ledger.LedgerRepository;
import com.example.wallet.shared.money.Currency;
import com.example.wallet.shared.money.Money;
import com.example.wallet.shared.pagination.PageResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public final class AccountService {
    private final AccountRepository accountRepository;
    private final ClientRepository clientrepository;
    private final LedgerRepository ledgerRepository;
    private final Clock clock;

    public AccountService(AccountRepository accountRepository, ClientRepository clientrepository, LedgerRepository ledgerRepository, Clock clock) {
        this.accountRepository = accountRepository;
        this.clientrepository = clientrepository;
        this.ledgerRepository = ledgerRepository;
        this.clock = clock;
    }

    public Account createAccount(UUID clientInitialId) {
        ClientId clientId = new ClientId(clientInitialId);
        checkClientExistence(clientId);

        Money money = new Money(Currency.RUB, BigDecimal.ZERO);
        Account account = new Account(
                new AccountId(UUID.randomUUID()),
                clientId,
                money,
                clock.instant()
        );
        accountRepository.save(account);

        return account;
    }

    public Account getAccountById(UUID accountInitialId) {
        AccountId accountId = new AccountId(accountInitialId);
        return accountRepository.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    public List<Account> getAccountsByClientId(UUID clientInitialId) {
        ClientId clientId = new ClientId(clientInitialId);
        checkClientExistence(clientId);

        return accountRepository.findAllByClientId(clientId);
    }

    public Account block(UUID accountInitialId) {
        AccountId accountId = new AccountId(accountInitialId);
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));

        account.block();
        accountRepository.save(account);
        return account;
    }

    public Account unblock(UUID accountInitialId) {
        AccountId accountId = new AccountId(accountInitialId);
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));

        account.unblock();
        accountRepository.save(account);
        return account;
    }

    public Account close(UUID accountInitialId) {
        AccountId accountId = new AccountId(accountInitialId);
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));

        account.close();
        accountRepository.save(account);
        return account;
    }

    public PageResponse<LedgerEntry> getHistory(UUID accountInitialId, LedgerEntriesQuery ledgerEntriesQuery) {
        AccountId accountId = new AccountId(accountInitialId);
        accountRepository.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));

        return ledgerRepository.findAllWithFilters(accountId, ledgerEntriesQuery);
    }

    private void checkClientExistence(ClientId clientId) {
        clientrepository.findById(clientId).orElseThrow(() -> new ClientNotFoundException(clientId));
    }
}
