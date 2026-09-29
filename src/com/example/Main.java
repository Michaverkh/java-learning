package com.example;

import com.example.wallet.transfers.TransferService;
import com.example.wallet.accounts.InMemoryAccountRepository;
import com.example.wallet.ledger.InMemoryLedgerRepository;
import com.example.wallet.ledger.LedgerEntryFactory;
import com.example.wallet.transfers.TransferFactory;
import com.example.wallet.transfers.InMemoryTransferRepository;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;

public class Main {

    public static void main(String[] args) {
        Supplier<UUID> idGenerator = UUID::randomUUID;
        Clock clock = Clock.systemUTC();

        TransferFactory transferFactory = new TransferFactory(idGenerator, clock);
        LedgerEntryFactory ledgerFactory = new LedgerEntryFactory(idGenerator, clock);

        TransferService transferService = new TransferService(
                transferFactory,
                ledgerFactory,
                new InMemoryTransferRepository(),
                new InMemoryAccountRepository(),
                new InMemoryLedgerRepository()
        );
    }
}
