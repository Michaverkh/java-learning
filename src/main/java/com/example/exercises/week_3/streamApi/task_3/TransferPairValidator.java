package com.example.exercises.week_3.streamApi.task_3;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Точка входа для задания 3. Реализуй проверку согласно условию в учебном материале.
 */
public final class TransferPairValidator {
    private final Set<OperationType> checkingExclusionsOperationType = Set.of(OperationType.DEPOSIT, OperationType.WITHDRAWAL);

    private Set<TransferPairViolation> validateGroup(List<LedgerEntry> entries) {
        Objects.requireNonNull(entries, "entries must not be null");

        EnumSet<TransferPairViolation> violations =
                EnumSet.noneOf(TransferPairViolation.class);

        int transferOutCount = 0;
        int transferInCount = 0;
        LedgerEntry transferOut = null;
        LedgerEntry transferIn = null;
        BigDecimal sum = BigDecimal.ZERO;

        for (LedgerEntry entry : entries) {
            Objects.requireNonNull(entry, "entry must not be null");

            sum = sum.add(entry.amount());

            if (entry.operationType() == OperationType.TRANSFER_OUT) {
                transferOutCount++;

                if (entry.amount().signum() >= 0) {
                    violations.add(
                            TransferPairViolation.INVALID_TRANSFER_OUT_AMOUNT_SIGN
                    );
                }

                if (transferOutCount == 1) {
                    transferOut = entry;
                }
            } else if (entry.operationType() == OperationType.TRANSFER_IN) {
                transferInCount++;

                if (entry.amount().signum() <= 0) {
                    violations.add(
                            TransferPairViolation.INVALID_TRANSFER_IN_AMOUNT_SIGN
                    );
                }

                if (transferInCount == 1) {
                    transferIn = entry;
                }
            }
        }

        if (entries.size() != 2) {
            violations.add(TransferPairViolation.WRONG_ENTRY_COUNT);
        }

        if (transferOutCount == 0) {
            violations.add(TransferPairViolation.MISSING_TRANSFER_OUT);
        }

        if (transferInCount == 0) {
            violations.add(TransferPairViolation.MISSING_TRANSFER_IN);
        }

        if (transferOutCount == 1
                && transferInCount == 1
                && transferOut.accountId().equals(transferIn.accountId())) {
            violations.add(TransferPairViolation.SAME_ACCOUNT);
        }

        if (sum.compareTo(BigDecimal.ZERO) != 0) {
            violations.add(TransferPairViolation.AMOUNTS_DO_NOT_BALANCE);
        }

        return Set.copyOf(violations);
    }

    public Map<UUID, Set<TransferPairViolation>> validate(List<LedgerEntry> entries) {
        Map<UUID, List<LedgerEntry>> groupByOperationId = entries.stream().filter(entry -> !checkingExclusionsOperationType.contains(entry.operationType())).collect(Collectors.groupingBy(LedgerEntry::operationId));

        return groupByOperationId.entrySet().stream().map(entry -> Map.entry(entry.getKey(), validateGroup(entry.getValue()))).filter(e -> !e.getValue().isEmpty()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
