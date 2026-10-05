package com.example.wallet.ledger;

import com.example.wallet.accounts.AccountId;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/** Проверки без сторонних библиотек: запуск через main. */
public final class LedgerTest {
    private static final AccountId SOURCE = new AccountId("00000000-0000-0000-0000-000000000001");
    private static final AccountId TARGET = new AccountId("00000000-0000-0000-0000-000000000002");
    private static final UUID OPERATION_ID = new UUID(0, 10);
    private static final Instant NOW = Instant.parse("2026-09-29T10:00:00Z");

    public static void main(String[] args) {
        for (OperationType type : OperationType.values()) {
            boolean incoming = type == OperationType.DEPOSIT || type == OperationType.TRANSFER_IN;
            String validAmount = incoming ? "10.00" : "-10.00";
            entry(1, SOURCE, type, validAmount, "20.00", NOW);
            expect(IllegalArgumentException.class,
                    () -> entry(1, SOURCE, type, incoming ? "-10.00" : "10.00", "20.00", NOW));
            expect(IllegalArgumentException.class,
                    () -> entry(1, SOURCE, type, "0.00", "20.00", NOW));
        }
        expect(IllegalArgumentException.class,
                () -> entry(1, SOURCE, OperationType.DEPOSIT, "1.001", "20.00", NOW));
        expect(IllegalArgumentException.class,
                () -> entry(1, SOURCE, OperationType.WITHDRAWAL, "-1.00", "-0.01", NOW));
        expect(IllegalArgumentException.class,
                () -> entry(1, SOURCE, OperationType.DEPOSIT, "1.00", "20.001", NOW));
        expect(NullPointerException.class,
                () -> entry(1, null, OperationType.DEPOSIT, "1.00", "20.00", NOW));

        LedgerEntry outgoing = entry(1, SOURCE, OperationType.TRANSFER_OUT, "-10.00", "0.00", NOW);
        LedgerEntry incoming = entry(2, TARGET, OperationType.TRANSFER_IN, "10.00", "10.00", NOW);
        LedgerRepository repository = new InMemoryLedgerRepository();
        repository.save(outgoing);
        repository.save(incoming);
        check(repository.findAllByOperationId(OPERATION_ID).equals(List.of(outgoing, incoming)),
                "operation must contain both transfer entries");
        check(outgoing.amount().add(incoming.amount()).signum() == 0, "transfer changes must sum to zero");

        List<LedgerEntry> snapshot = repository.findAllByAccountId(SOURCE);
        expect(UnsupportedOperationException.class, () -> snapshot.add(incoming));
        expect(UnsupportedOperationException.class,
                () -> repository.findAllByOperationId(OPERATION_ID).clear());
        LedgerEntry newer = entry(3, SOURCE, OperationType.DEPOSIT, "5.00", "5.00", NOW.plusSeconds(1));
        repository.save(newer);
        check(snapshot.equals(List.of(outgoing)), "history snapshot must remain unchanged");
        check(repository.findAllByAccountId(SOURCE).equals(List.of(newer, outgoing)),
                "history must be ordered newest first and filtered by account");

        LedgerEntry replacement = entry(1, TARGET, OperationType.DEPOSIT, "100.00", "100.00", NOW);
        expect(IllegalStateException.class, () -> repository.save(replacement));
        check(repository.findById(outgoing.id()).orElseThrow().equals(outgoing),
                "duplicate id must not replace the original entry");
        UUID unknown = new UUID(0, 99);
        check(repository.findById(unknown).isEmpty(), "unknown entry must be absent");
        check(repository.findAllByOperationId(unknown).isEmpty(), "unknown operation must be empty");
        check(repository.findAllByAccountId(new AccountId(unknown.toString())).isEmpty(),
                "unknown account history must be empty");

        LedgerEntryFactory factory = new LedgerEntryFactory(() -> incoming.id(), Clock.fixed(NOW, ZoneOffset.UTC));
        check(factory.create(TARGET, OPERATION_ID, OperationType.TRANSFER_IN,
                new BigDecimal("10.00"), new BigDecimal("10.00")).equals(incoming),
                "factory must use supplied id generator and clock");
        System.out.println("Ledger checks passed");
    }

    private static LedgerEntry entry(long id, AccountId account, OperationType type,
                                     String amount, String balance, Instant time) {
        return new LedgerEntry(new UUID(0, id), account, OPERATION_ID, type,
                new BigDecimal(amount), new BigDecimal(balance), time);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expect(Class<? extends RuntimeException> expected, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            if (expected.isInstance(exception)) {
                return;
            }
            throw new AssertionError("Unexpected exception", exception);
        }
        throw new AssertionError("Expected " + expected.getSimpleName());
    }
}
