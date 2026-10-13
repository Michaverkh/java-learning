package com.example.wallet.transfers;

import com.example.wallet.accounts.Account;
import com.example.wallet.accounts.AccountRepository;
import com.example.wallet.accounts.exceptions.AccountNotActiveException;
import com.example.wallet.accounts.exceptions.AccountNotFoundException;
import com.example.wallet.accounts.exceptions.InsufficientFundsException;
import com.example.wallet.ledger.LedgerEntry;
import com.example.wallet.ledger.LedgerEntryFactory;
import com.example.wallet.ledger.LedgerRepository;
import com.example.wallet.ledger.OperationType;
import com.example.wallet.shared.money.Currency;
import com.example.wallet.shared.money.Money;
import com.example.wallet.transfers.exceptions.InvalidTransferException;
import com.example.wallet.transfers.exceptions.SameAccountTransferException;
import com.example.wallet.transfers.exceptions.TransferCreationException;
import com.example.wallet.transfers.exceptions.TransferCurrencyMismatchException;
import com.example.wallet.transfers.exceptions.TransferNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Service
public final class TransferService {
    private final TransferFactory transferFactory;
    private final LedgerEntryFactory ledgerFactory;
    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final LedgerRepository ledgerRepository;

    private final TransferLimitPolicy transferLimitPolicy;

    public TransferService(
            TransferFactory transferFactory,
            LedgerEntryFactory ledgerFactory,
            TransferRepository transferRepository,
            AccountRepository accountRepository,
            LedgerRepository ledgerRepository,
            TransferLimitPolicy transferLimitPolicy
    ) {
        this.transferFactory = transferFactory;
        this.ledgerFactory = ledgerFactory;

        this.transferRepository = transferRepository;
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;

        this.transferLimitPolicy = transferLimitPolicy;
    }

    public Transfer getTransferById(UUID transferId) {
        return transferRepository.findById(transferId).orElseThrow(TransferNotFoundException::new);
    }


    public Transfer createTransfer(TransferCommand transferCommand) {
        if (transferCommand == null) {
            throw new InvalidTransferException("Параметры перевода обязательны");
        }
        if (transferCommand.sourceAccountId() == null || transferCommand.targetAccountId() == null) {
            throw new InvalidTransferException("Идентификаторы обоих счетов обязательны");
        }
        if (transferCommand.currency() == null) {
            throw new InvalidTransferException("Валюта перевода обязательна");
        }
        BigDecimal amount = transferCommand.amount();
        if (amount == null) {
            throw new InvalidTransferException("Сумма перевода обязательна");
        }

        if (amount.signum() <= 0) {
            throw new InvalidTransferException("Сумма перевода должна быть положительной");
        }

        if (amount.scale() > 2) {
            throw new InvalidTransferException(
                    "Сумма перевода должна содержать не более двух дробных знаков"
            );
        }

        transferLimitPolicy.check(amount);

        if (Objects.equals(
                transferCommand.sourceAccountId(),
                transferCommand.targetAccountId()
        )) {
            throw new SameAccountTransferException();
        }

        Account sourceAccount = accountRepository
                .findById(transferCommand.sourceAccountId())
                .orElseThrow(() -> new AccountNotFoundException(
                        transferCommand.sourceAccountId()
                ));
        Account targetAccount = accountRepository
                .findById(transferCommand.targetAccountId())
                .orElseThrow(() -> new AccountNotFoundException(
                        transferCommand.targetAccountId()
                ));

        if (!sourceAccount.isActive() || !targetAccount.isActive()) {
            throw new AccountNotActiveException();
        }

        if (sourceAccount.getBalance().getCurrency() != Currency.RUB
                || targetAccount.getBalance().getCurrency() != Currency.RUB
                || transferCommand.currency() != Currency.RUB) {
            throw new TransferCurrencyMismatchException();
        }

        BigDecimal sourceBalanceAfter = sourceAccount.getBalance().getAmount().subtract(amount);

        if (sourceBalanceAfter.signum() < 0) {
            throw new InsufficientFundsException();
        }

        BigDecimal targetBalanceAfter = targetAccount.getBalance().getAmount().add(amount);

        Transfer transfer = transferFactory.create(transferCommand);
        if (transfer.id() == null) {
            throw new TransferCreationException("transfer id must not be null");
        }

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
            throw new TransferCreationException("transfer with this id already exists");
        }

        if (outgoingEntry.id().equals(incomingEntry.id())
                || ledgerRepository.findById(outgoingEntry.id()).isPresent()
                || ledgerRepository.findById(incomingEntry.id()).isPresent()) {
            throw new TransferCreationException("ledger entry ids must be unique");
        }

        // Учебная версия: один поток и хранилища в памяти без сбоев записи.
        // Проверки выше не заменяют транзакцию при переходе к базе данных.
        sourceAccount.withdraw(transferAmount);
        targetAccount.deposit(transferAmount);
        transferRepository.save(transfer);
        ledgerRepository.save(outgoingEntry);
        ledgerRepository.save(incomingEntry);

        return transfer;
    }
}
