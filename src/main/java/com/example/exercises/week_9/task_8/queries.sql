-- 1. Пронумеруй операции отдельно для каждого счёта от новых к старым.
    SELECT account_id, id, amount,
           row_number() OVER (
               PARTITION BY account_id
               ORDER BY created_at DESC, id DESC
           ) AS position
    FROM ledger_entry
    ORDER BY account_id, position;

-- 2. Получи последнюю запись каждого счёта с историей: две строки, балансы 700 и 350.
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

-- 3. Дополни результат пустым A3 через `LEFT JOIN`.
WITH numbered AS (
    SELECT e.*, row_number() OVER (
        PARTITION BY account_id ORDER BY created_at DESC, id DESC
    ) AS position
    FROM ledger_entry e
    )
SELECT a.id, n.operation_type, n.balance_after
FROM account a
LEFT JOIN numbered n
        ON n.account_id = a.id
        AND n.position = 1
ORDER BY a.id;

-- 4. Вычисли накопленные суммы с явной рамкой `ROWS`: для A1 — 1000, 750, 700; для A2 — 100, 350, 300, 350.
SELECT account_id, id, amount, balance_after,
       sum(amount) OVER (
           PARTITION BY account_id
           ORDER BY created_at, id
           ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
       ) AS calculated_balance
FROM ledger_entry
ORDER BY account_id, created_at, id;

-- 5. Получи предыдущий `balance_after` через `LAG`. Объясни, почему у первой записи `NULL`.
SELECT account_id, id, balance_after,
       lag(balance_after) OVER (
           PARTITION BY account_id ORDER BY created_at, id
       ) AS previous_balance
FROM ledger_entry
ORDER BY account_id, created_at, id;

-- 6. Покажи только операции начиная со 2 октября, сохранив в расчёте более ранние пополнения.
WITH calculated AS (
    SELECT account_id, id, balance_after, created_at,
       lag(balance_after) OVER (
           PARTITION BY account_id ORDER BY created_at, id
       ) AS previous_balance
    FROM ledger_entry
    )
SELECT *
FROM calculated c
WHERE c.created_at >= '2026-10-02T00:00:00Z'::timestamptz
ORDER BY account_id, created_at, id;
-- 7. Объясни, почему перенос фильтра периода внутрь запроса с окном изменяет результат.
-- 8. Почему сортировка по `created_at, id` недостаточна для доказательства настоящего порядка конкурентных проводок?