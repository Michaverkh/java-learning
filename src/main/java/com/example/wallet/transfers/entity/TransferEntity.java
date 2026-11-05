package com.example.wallet.transfers.entity;

import com.example.wallet.shared.money.Currency;
import com.example.wallet.transfers.TransferStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Entity
@Table(name = "transfer")
public class TransferEntity {

    @Id
    private UUID id;

    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    @Column(name = "target_account_id", nullable = false)
    private UUID targetAccountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Column(name = "description")
    private String description;

    protected TransferEntity() {
        // Для Hibernate.
    }

    public TransferEntity(UUID id, UUID sourceAccountId, UUID targetAccountId, Currency currency, BigDecimal amount,
                          TransferStatus status, Instant createdAt, Instant completedAt, String description) {
        this.id = requireNonNull(id);
        this.sourceAccountId = requireNonNull(sourceAccountId);
        this.targetAccountId = requireNonNull(targetAccountId);
        this.amount = requireNonNull(amount);
        this.status = requireNonNull(status);
        this.currency = requireNonNull(currency);
        this.createdAt = requireNonNull(createdAt);
        this.completedAt = requireNonNull(completedAt);

        this.description = description;
    }
}