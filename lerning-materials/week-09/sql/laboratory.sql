
psql -X -h localhost -U wallet_student -d wallet_learning
\cd D:/my-projects/education/java-learning

\r
ROLLBACK;

BEGIN;
SET LOCAL search_path = pg_temp;
SET LOCAL TIME ZONE 'UTC';

\i lerning-materials/week-09/sql/schema.sql
\i lerning-materials/week-09/sql/seed.sql

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