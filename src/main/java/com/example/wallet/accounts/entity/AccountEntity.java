package com.example.wallet.accounts.entity;

import com.example.wallet.accounts.AccountStatus;
import com.example.wallet.shared.money.Currency;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Entity
@Table(name = "account")
public class AccountEntity {

    @Id
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AccountEntity() {
        // Для Hibernate.
    }

    public AccountEntity(UUID id, UUID clientId, Currency currency, Instant createdAt) {
        this.id = requireNonNull(id);
        this.clientId = requireNonNull(clientId);
        this.currency = requireNonNull(currency);
        this.balance = BigDecimal.ZERO.setScale(2);
        this.status = AccountStatus.ACTIVE;
        this.createdAt = requireNonNull(createdAt);
        this.updatedAt = createdAt;
    }
}