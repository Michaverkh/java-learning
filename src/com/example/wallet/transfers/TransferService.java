package com.example.wallet.transfers;

import com.example.wallet.accounts.Account;
import com.example.wallet.accounts.AccountRepository;
import com.example.wallet.ledger.LedgerEntry;
import com.example.wallet.ledger.LedgerEntryFactory;
import com.example.wallet.ledger.LedgerRepository;
import com.example.wallet.ledger.OperationType;
import com.example.wallet.shared.money.Currency;
import com.example.wallet.shared.money.Money;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public final class TransferService {
    private final TransferFactory transferFactory;
    private final LedgerEntryFactory ledgerFactory;
    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final LedgerRepository ledgerRepository;


    public TransferService(
            TransferFactory transferFactory,
            LedgerEntryFactory ledgerFactory,
            TransferRepository transferRepository,
            AccountRepository accountRepository,
            LedgerRepository ledgerRepository
    ) {
        this.transferFactory = transferFactory;
        this.ledgerFactory = ledgerFactory;

        this.transferRepository = transferRepository;
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
    }

    public void createTransfer(TransferCommand transferCommand) {
        Objects.requireNonNull(transferCommand, "transferCommand must not be null");

        BigDecimal amount = Objects.requireNonNull(
                transferCommand.amount(),
                "transfer amount must not be null"
        );

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("transfer amount must be positive");
        }

        if (amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "transfer amount must have at most two decimal places"
            );
        }

        if (Objects.equals(
                transferCommand.sourceAccountId(),
                transferCommand.targetAccountId()
        )) {
            throw new IllegalArgumentException(
                    "source and target accounts must be different"
            );
        }

        Account sourceAccount = accountRepository
                .findById(transferCommand.sourceAccountId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "source account was not found"
                ));
        Account targetAccount = accountRepository
                .findById(transferCommand.targetAccountId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "target account was not found"
                ));

        if (!sourceAccount.getIsActive() || !targetAccount.getIsActive()) {
            throw new IllegalStateException("both accounts must be active");
        }

        if (sourceAccount.getBalance().getCurrency() != Currency.RUB
                || targetAccount.getBalance().getCurrency() != Currency.RUB
                || transferCommand.currency() != Currency.RUB) {
            throw new IllegalArgumentException("only RUB transfers are supported");
        }

        BigDecimal sourceBalanceAfter = sourceAccount.getBalance().getAmount().subtract(amount);

        if (sourceBalanceAfter.signum() < 0) {
            throw new IllegalStateException("insufficient funds");
        }

        BigDecimal targetBalanceAfter = targetAccount.getBalance().getAmount().add(amount);

        Transfer transfer = transferFactory.create(transferCommand);
        Objects.requireNonNull(transfer.id(), "transfer id must not be null");

        Money transferAmount = new Money(transferCommand.currency(), amount);

        LedgerEntry outgoingEntry = ledgerFactory.create(
                sourceAccount.id(), transfer.id(), OperationType.TRANSFER_OUT,
                amount.negate(), sourceBalanceAfter
        );
        LedgerEntry incomingEntry = ledgerFactory.create(
                targetAccount.id(), transfer.id(), OperationType.TRANSFER_IN,
                amount, targetBalanceAfter
        );

        if (transferRepository.findById(transfer.id()).isPresent()) {
            throw new IllegalStateException("transfer with this id already exists");
        }

        if (outgoingEntry.id().equals(incomingEntry.id())
                || ledgerRepository.findById(outgoingEntry.id()).isPresent()
                || ledgerRepository.findById(incomingEntry.id()).isPresent()) {
            throw new IllegalStateException("ledger entry ids must be unique");
        }

        // Учебная версия: один поток и хранилища в памяти без сбоев записи.
        // Проверки выше не заменяют транзакцию при переходе к базе данных.
        sourceAccount.withdraw(transferAmount);
        targetAccount.deposit(transferAmount);
        transferRepository.save(transfer);
        ledgerRepository.save(outgoingEntry);
        ledgerRepository.save(incomingEntry);
    }
}
