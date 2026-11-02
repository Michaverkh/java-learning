package com.example.wallet.ledger.persistence;

import com.example.wallet.accounts.AccountId;
import com.example.wallet.ledger.LedgerEntriesQuery;
import com.example.wallet.ledger.LedgerEntry;
import com.example.wallet.ledger.LedgerHistoryReader;
import com.example.wallet.ledger.OperationType;
import com.example.wallet.shared.pagination.PageResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class JdbcLedgerHistoryReader implements LedgerHistoryReader {
    private final JdbcTemplate jdbcTemplate;

    public JdbcLedgerHistoryReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PageResponse<LedgerEntry> findHistory(
            AccountId accountId,
            LedgerEntriesQuery query
    ) {
        if (query.page() < 0) {
            throw new IllegalArgumentException("page must be non-negative");
        }
        if (query.size() < 1 || query.size() > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        if (query.dateFrom() != null
                && query.dateTo() != null
                && !query.dateFrom().isBefore(query.dateTo())) {
            throw new IllegalArgumentException("dateFrom must be before dateTo");
        }
        
        StringBuilder whereClause = new StringBuilder("""
                WHERE account_id = ?
                """);

        List<Object> filterArguments = new ArrayList<>();
        filterArguments.add(accountId.getValue());

        if (query.operationType() != null) {
            whereClause.append(" AND operation_type = ?");
            filterArguments.add(query.operationType().name());
        }

        if (query.dateFrom() != null) {
            whereClause.append(" AND created_at >= ?");
            filterArguments.add(Timestamp.from(query.dateFrom()));
        }

        if (query.dateTo() != null) {
            whereClause.append(" AND created_at < ?");
            filterArguments.add(Timestamp.from(query.dateTo()));
        }

        List<Object> pageArguments = new ArrayList<>(filterArguments);
        pageArguments.add(query.size());
        pageArguments.add((long) query.page() * query.size());

        List<LedgerEntry> items = jdbcTemplate.query("""
                        SELECT id,
                               account_id,
                               operation_id,
                               operation_type,
                               amount,
                               balance_after,
                               created_at
                        FROM ledger_entry
                        ""\" + whereClause + ""\"
                        ORDER BY created_at DESC, id DESC
                        LIMIT ?
                        OFFSET ?
                        """,
                (resultSet, rowNumber) -> new LedgerEntry(
                        resultSet.getObject("id", UUID.class),
                        new AccountId(resultSet.getObject("account_id", UUID.class)),
                        resultSet.getObject("operation_id", UUID.class),
                        OperationType.valueOf(resultSet.getString("operation_type")),
                        resultSet.getBigDecimal("amount"),
                        resultSet.getBigDecimal("balance_after"),
                        resultSet.getObject("created_at", OffsetDateTime.class).toInstant()
                ),
                pageArguments.toArray()
        );

        long totalElements = Objects.requireNonNull(jdbcTemplate.queryForObject("""
                                SELECT COUNT(*)
                                FROM ledger_entry
                                ""\" + whereClause + ""\"
                                """,
                        Long.class,
                        filterArguments.toArray()
                ), "COUNT(*) must return a value"
        );

        long totalPages = Math.ceilDiv(totalElements, (long) query.size());

        return new PageResponse<>(items, query.page(), query.size(), totalElements, totalPages);
    }
}
