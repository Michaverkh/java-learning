-- 4. SELECT, NULL и границы периода
-- Получи все счета Анны, отсортированные по id. Ожидаются A1 и A3

SELECT *
FROM account a
WHERE a.client_id = '10000000-0000-0000-0000-000000000001'
ORDER BY a.id;

-- Получи перевод без описания. Ожидается перевод на 250.

SELECT id, description
FROM transfer t
WHERE t.description IS NULL;

-- Получи переводы, описание которых отличается от `Возврат за обед`, включая отсутствие описания.

SELECT id, description
FROM transfer
WHERE description IS DISTINCT FROM 'Возврат за обед';

-- Получи историю A2 за период `[2026-10-02T00:00:00Z, 2026-10-03T00:00:00Z)`.

SELECT id, amount
FROM ledger_entry
WHERE account_id = '20000000-0000-0000-0000-000000000002'
    AND created_at >= '2026-10-02T00:00:00Z'::timestamptz
    AND created_at <  '2026-10-03T00:00:00Z'::timestamptz;

-- Выведи историю A1, новые записи первыми. При одинаковом времени используй `id` как второй ключ.

SELECT id, amount, created_at
FROM ledger_entry
WHERE account_id = '20000000-0000-0000-0000-000000000001'
ORDER BY created_at, id;