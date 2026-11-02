-- 1. История A1: три строки, новые первыми.
SELECT id, operation_id, operation_type, amount, balance_after, created_at
FROM ledger_entry
WHERE account_id = '20000000-0000-0000-0000-000000000001'
ORDER BY created_at DESC, id DESC
LIMIT 20;

-- 2. Обороты, включая пустой A3: A1 (1000, 300), A2 (400, 50), A3 (0, 0).
SELECT a.id, a.currency, count(e.id) AS entry_count,
       coalesce(sum(e.amount) FILTER (WHERE e.amount > 0), 0) AS incoming,
       coalesce(-sum(e.amount) FILTER (WHERE e.amount < 0), 0) AS outgoing
FROM account a
LEFT JOIN ledger_entry e ON e.account_id = a.id
GROUP BY a.id, a.currency
ORDER BY a.id;

-- 3. Последняя запись каждого непустого счёта: A1 700, A2 350.
WITH numbered AS (
    SELECT e.*, row_number() OVER (
        PARTITION BY account_id ORDER BY created_at DESC, id DESC
    ) AS position
    FROM ledger_entry e
)
SELECT account_id, operation_type, balance_after
FROM numbered
WHERE position = 1
ORDER BY account_id;

-- 4. Накопленная сумма. Учебный набор содержит полную историю от нуля.
SELECT account_id, id, amount, balance_after,
       sum(amount) OVER (
           PARTITION BY account_id ORDER BY created_at, id
           ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
       ) AS calculated_balance
FROM ledger_entry
ORDER BY account_id, created_at, id;

-- 5. Несогласованные текущие балансы: ожидается ноль строк.
SELECT a.id, a.balance, coalesce(sum(e.amount), 0) AS ledger_balance
FROM account a
LEFT JOIN ledger_entry e ON e.account_id = a.id
GROUP BY a.id, a.balance
HAVING a.balance <> coalesce(sum(e.amount), 0);

-- 6. Проверка состава переводов: ожидается ноль строк.
-- Помимо суммы проверяем число, тип, счёт и величину обеих записей.
SELECT t.id
FROM transfer t
LEFT JOIN ledger_entry e ON e.operation_id = t.id
GROUP BY t.id, t.source_account_id, t.target_account_id, t.amount
HAVING count(e.id) <> 2
    OR coalesce(sum(e.amount), 0) <> 0
    OR count(e.id) FILTER (
        WHERE e.operation_type = 'TRANSFER_OUT'
          AND e.account_id = t.source_account_id AND e.amount = -t.amount
    ) <> 1
    OR count(e.id) FILTER (
        WHERE e.operation_type = 'TRANSFER_IN'
          AND e.account_id = t.target_account_id AND e.amount = t.amount
    ) <> 1;
