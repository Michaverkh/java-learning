-- 5. Соединения и пустые группы

-- 1. Получи все счета с именами владельцев: три строки.
SELECT *
FROM account a
         LEFT JOIN client c ON a.client_id = c.id AND c.name IS NOT NULL;

-- 2. Получи всех клиентов с их счетами через `LEFT JOIN`: четыре строки, включая Веру с пустыми полями счёта.
SELECT c.id, a.client_id, c.name
FROM client c
         LEFT JOIN account a ON a.client_id = c.id;

-- 3. Получи все счета и только их пополнения, сохранив A3: три строки.

SELECT *
FROM account a
         LEFT JOIN ledger_entry e
                   ON a.id = e.account_id AND (e.operation_type = 'DEPOSIT' OR e.operation_type = 'TRANSFER_IN');

-- 4. Перенеси условие о типе пополнения из `ON` в `WHERE`. Объясни, почему теперь две строки.
-- Не совсем понял задачу

-- 5. Соедини A1 одновременно с журналом и исходящими переводами. Предскажи и проверь шесть строк.
SELECT *
FROM account a
         JOIN ledger_entry e
              ON a.id = e.account_id
         JOIN transfer t
              ON a.id = t.source_account_id;

-- 6. Объясни, почему `DISTINCT` не является универсальным исправлением ошибки из предыдущего пункта.
-- `DISTINCT` удаляет одинаковые строки по всем выбранным выражениям. Он полезен, когда нужен именно набор уникальных
-- значений, но не исправляет неверное соединение.

