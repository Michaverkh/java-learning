-- ## 9. Свяжи SQL с будущим репозиторием
--
-- Опиши SQL для метода чтения истории без реализации JDBC:
--
-- - вход: идентификатор счёта, начало и конец периода, размер страницы и смещение;
-- - выход: идентификатор записи, идентификатор операции, тип, знаковая сумма, баланс после операции, время, необязательное
--   назначение перевода;
-- - пополнения и списания остаются в результате;
-- - порядок однозначен, размер результата ограничен;
-- - значения пользователя будут передаваться параметрами;
-- - право чтения счёта должно проверяться отдельно либо выражаться дополнительными условиями доступа.

SELECT e.id,
       e.operation_id,
       e.operation_type,
       e.amount,
       e.balance_after,
       e.created_at,
       t.description
FROM ledger_entry e
LEFT JOIN transfer t
       ON t.id = e.operation_id
      AND e.operation_type IN ('TRANSFER_IN', 'TRANSFER_OUT')
WHERE e.account_id = CAST(:accountId AS uuid)
  AND e.created_at >= CAST(:periodStart AS timestamptz)
  AND e.created_at < CAST(:periodEnd AS timestamptz)
ORDER BY e.created_at DESC, e.id DESC
LIMIT :pageSize
OFFSET :offset;
