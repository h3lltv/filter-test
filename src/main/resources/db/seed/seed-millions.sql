-- Manual-only bulk seed script for local scale/memory testing.
-- Not a Flyway migration (lives outside db/migration) and never run automatically by the app or tests.
--
-- Usage:
--   psql -h localhost -U shop -d shop -f src/main/resources/db/seed/seed-millions.sql
--
-- Inserts 5,000,000 rows cycling through a handful of name prefixes so that the
-- TASK.md example filters (^E.*$ and ^.*[eva].*$) have a realistic match/non-match split.

INSERT INTO products (id, name, description)
SELECT
    n AS id,
    (ARRAY['Eagle', 'Vortex', 'Anvil', 'Widget', 'Zephyr', 'Falcon', 'Ibis', 'Nomad'])[1 + (n % 8)] || '-' || n AS name,
    'Description for product ' || n AS description
FROM generate_series(1, 5000000) AS n
ON CONFLICT (id) DO NOTHING;
