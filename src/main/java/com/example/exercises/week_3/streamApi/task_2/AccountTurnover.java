package com.example.exercises.week_3.streamApi.task_2;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Результат расчёта оборотов одного счёта за выбранный набор записей.
 */
public record AccountTurnover(
        BigDecimal incoming,
        BigDecimal outgoing,
        BigDecimal netChange
) {
    public AccountTurnover {
        Objects.requireNonNull(incoming, "incoming must not be null");
        Objects.requireNonNull(outgoing, "outgoing must not be null");
        Objects.requireNonNull(netChange, "netChange must not be null");

        if (incoming.signum() < 0) {
            throw new IllegalArgumentException("incoming must not be negative");
        }
        if (outgoing.signum() < 0) {
            throw new IllegalArgumentException("outgoing must not be negative");
        }
    }

    public static AccountTurnover zero() {
        BigDecimal zero = new BigDecimal("0.00");
        return new AccountTurnover(zero, zero, zero);
    }

    public AccountTurnover add(AccountTurnover other) {
        return new AccountTurnover(incoming.add(other.incoming), outgoing.add(other.outgoing), netChange.add(other.netChange()));
    }
}
