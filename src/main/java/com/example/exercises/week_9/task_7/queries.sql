-- ## 7. Подзапросы и диагностические запросы
--
-- 1. Через `NOT EXISTS` найди счета без операций: A3.
SELECT a.id
FROM account a
WHERE NOT EXISTS (SELECT 1
                  FROM ledger_entry e
                  WHERE e.account_id = a.id)
ORDER BY a.id;

-- 2. Через `NOT EXISTS` найди клиентов без счетов: Вера.
SELECT c.id, name
FROM client c
WHERE NOT EXISTS (SELECT 1
                  FROM account a
                  WHERE a.client_id = c.id)
ORDER BY c.id;

-- 3. Найди переводы, для которых нет ровно одной исходящей записи на `-transfer.amount` для отправителя и одной входящей
--    на `+transfer.amount` для получателя. Проверь также общее число записей и нулевую сумму.
SELECT t.id
FROM transfer t
WHERE (SELECT count(*)
       FROM ledger_entry e
       WHERE e.operation_id = t.id
         AND e.account_id = t.source_account_id
         AND e.operation_type = 'TRANSFER_OUT'
         AND e.amount = -t.amount) <> 1
   OR (SELECT count(*)
       FROM ledger_entry e
       WHERE e.operation_id = t.id
         AND e.account_id = t.target_account_id
         AND e.operation_type = 'TRANSFER_IN'
         AND e.amount = t.amount) <> 1
ORDER BY t.id;

-- 4. Напиши обратную проверку: найди записи типов `TRANSFER_IN` и `TRANSFER_OUT`, для которых нет строки `transfer`.
SELECT e.id, operation_type
FROM ledger_entry e
WHERE (e.operation_type = 'TRANSFER_IN'
    or e.operation_type = 'TRANSFER_OUT')
  AND NOT EXISTS (SELECT 1
                  FROM transfer t
                  WHERE e.operation_id = t.id)
ORDER BY e.id;
-- 5. В отдельном эксперименте добавь корректную по локальным ограничениям запись `TRANSFER_IN` с несуществующим
--    идентификатором перевода и убедись, что запрос из пункта 4 находит её. Откати эксперимент.