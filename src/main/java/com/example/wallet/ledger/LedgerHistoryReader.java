package com.example.wallet.ledger;

import com.example.wallet.accounts.AccountId;
import com.example.wallet.shared.pagination.PageResponse;

public interface LedgerHistoryReader {
    PageResponse<LedgerEntry> findHistory(
            AccountId accountId,
            LedgerEntriesQuery query
    );
}
