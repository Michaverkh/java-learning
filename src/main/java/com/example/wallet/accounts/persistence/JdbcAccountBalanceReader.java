package com.example.wallet.accounts.persistence;

import com.example.wallet.accounts.AccountBalanceReader;
import com.example.wallet.accounts.AccountId;
import com.example.wallet.shared.money.Currency;
import com.example.wallet.shared.money.Money;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JdbcAccountBalanceReader implements AccountBalanceReader {
    private final JdbcTemplate jdbcTemplate;

    public JdbcAccountBalanceReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Money> findBalanceByAccountId(AccountId accountId) {
        return jdbcTemplate.query("""
                        select balance, currency
                        from account
                        where id = ?
                        """,
                (resultSet, rowNumber) -> new Money(
                        Currency.valueOf(resultSet.getString("currency")),
                        resultSet.getBigDecimal("balance")
                ),
                accountId.getValue()
        ).stream().findFirst();
    }
}
