-- ## 6. Обороты и сверка баланса
--
-- 1. Для каждого счёта верни число записей, приход и положительный расход, включая A3 с нулями.
SELECT a.id AS account_id,
       count(e.id) AS entry_count,
       coalesce(sum(e.amount) FILTER (WHERE e.amount > 0), 0) AS incoming,
       coalesce(-sum(e.amount) FILTER (WHERE e.amount < 0), 0) AS outgoing
FROM account a
         LEFT JOIN ledger_entry e
         ON e.account_id = a.id
GROUP BY a.id
ORDER BY a.id;

-- 2. Повтори расчёт только за 2 октября в UTC. Ожидаемые пары «приход / расход»:
--    A1 — 0 / 250, A2 — 250 / 50, A3 — 0 / 0.
SELECT a.id AS account_id,
       count(e.id) AS entry_count,
       coalesce(sum(e.amount) FILTER (WHERE e.amount > 0), 0) AS incoming,
       coalesce(-sum(e.amount) FILTER (WHERE e.amount < 0), 0) AS outgoing
FROM account a
         LEFT JOIN ledger_entry e
                   ON e.account_id = a.id
                       AND e.created_at >= '2026-10-02T00:00:00Z'::timestamptz
      AND e.created_at <  '2026-10-03T00:00:00Z'::timestamptz
GROUP BY a.id
ORDER BY a.id;

-- 3. Через `HAVING` выбери счета, имеющие минимум четыре записи: только A2.
SELECT a.id AS account_id,
       count(e.id) AS entry_count,
       coalesce(sum(e.amount) FILTER (WHERE e.amount > 0), 0) AS incoming,
       coalesce(-sum(e.amount) FILTER (WHERE e.amount < 0), 0) AS outgoing
FROM account a
         LEFT JOIN ledger_entry e
                   ON e.account_id = a.id
GROUP BY a.id
HAVING count(*) >= 4
ORDER BY a.id;

-- 4. Посчитай внешние пополнения отдельно от входящих переводов: 1100 и 300 соответственно.
SELECT e.operation_type,
       coalesce(sum(e.amount), 0) AS value
FROM ledger_entry e
WHERE e.operation_type='DEPOSIT' OR e.operation_type='TRANSFER_IN'
GROUP BY e.operation_type;

-- 5. Сравни `account.balance` с суммой полной истории и верни только расхождения: ноль строк.
SELECT a.id AS account_id,
       a.balance AS balance,
       coalesce(sum(e.amount), 0) AS calculated_balance
FROM account a
         LEFT JOIN ledger_entry e
                   ON e.account_id = a.id
GROUP BY a.id
HAVING  a.balance <> coalesce(sum(e.amount), 0)
ORDER BY a.id;
