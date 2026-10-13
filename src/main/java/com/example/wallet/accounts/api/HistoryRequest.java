package com.example.wallet.accounts.api;

import com.example.wallet.ledger.LedgerEntriesQuery;
import com.example.wallet.ledger.OperationType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

public record HistoryRequest(
        @Min(0)
        Integer page,

        @Min(1) @Max(100)
        Integer size,

        OperationType operationType,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant dateFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant dateTo
) {
    public HistoryRequest {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }

    @AssertTrue(message = "dateFrom должна быть раньше dateTo")
    public boolean isDateRangeValid() {
        return dateFrom == null
                || dateTo == null
                || dateFrom.isBefore(dateTo);
    }

    public LedgerEntriesQuery toQuery() {
        return new LedgerEntriesQuery(
                page, size, operationType, dateFrom, dateTo
        );
    }
}