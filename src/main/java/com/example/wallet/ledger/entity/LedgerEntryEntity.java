package com.example.wallet.ledger.entity;

import com.example.wallet.ledger.OperationType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Entity
@Table(name = "ledger_entry")
public class LedgerEntryEntity {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private OperationType operationType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LedgerEntryEntity() {
        // Для Hibernate.
    }

    public LedgerEntryEntity(UUID id, UUID accountId, UUID operationId, OperationType operationType, BigDecimal amount, BigDecimal balanceAfter,
                             Instant createdAt) {
        this.id = requireNonNull(id);
        this.accountId = requireNonNull(accountId);
        this.operationId = requireNonNull(operationId);
        this.amount = requireNonNull(amount);
        this.operationType = requireNonNull(operationType);
        this.balanceAfter = requireNonNull(balanceAfter);
        this.createdAt = requireNonNull(createdAt);
    }
}