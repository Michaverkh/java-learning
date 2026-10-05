package com.example.wallet.ledger;

import com.example.wallet.accounts.AccountId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LedgerRepository {
    /** Добавляет запись. Повторный id запрещён, обновление записей не поддерживается. */
    void save(LedgerEntry entry);

    Optional<LedgerEntry> findById(UUID id);

    /** Возвращает неизменяемый снимок истории: сначала новые записи, затем по id. */
    List<LedgerEntry> findAllByAccountId(AccountId accountId);

    /** Возвращает неизменяемый список записей операции по времени, затем по id. */
    List<LedgerEntry> findAllByOperationId(UUID operationId);
}
