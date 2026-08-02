# shop

A Spring Boot (WebFlux + R2DBC) service that streams `products` from PostgreSQL and excludes rows
whose `name` matches a caller-supplied regular expression, filtering entirely in application code.

## Running locally

```
docker compose up -d
./gradlew bootRun
```

The app starts on `http://localhost:8080`. Flyway creates the `products` table automatically on
startup; the table starts empty.

## Endpoints

Both endpoints take the same query parameters and return a JSON array of products whose `name` does
**not** match the pattern. They differ only in which regex engine evaluates the pattern.

- `nameFilter` (required) — the exclusion pattern.
- `limit` (optional) — caps the number of matching rows returned, applied after filtering.
- `offset` (optional, default `0`) — resumes from a cursor: only rows with `id` greater than this
  are considered. Pass the `id` of the last row from a previous page to fetch the next one; this is
  pushed down as an indexed `WHERE id > :offset` seek rather than re-scanning rows from the start.

### `GET /shop/product?nameFilter=<regex>`

Uses `java.util.regex` — full Java regular expression syntax (backreferences, lookaround, etc.).
Each match is bounded by a configurable per-match deadline (`shop.matcher.regex.match-timeout`,
default 50ms) to limit the worst-case cost of a pathological pattern; if a single name's evaluation
exceeds the deadline, that row is excluded and a warning is logged, but the request still completes.

```
curl "http://localhost:8080/shop/product?nameFilter=^E.*\$"
```

### `GET /shop/product/re2?nameFilter=<regex>`

Uses [RE2J](https://github.com/google/re2j), which compiles patterns to a finite automaton and
matches in linear time, structurally immune to catastrophic backtracking. It does not support
backreferences or lookahead/lookbehind — such patterns are rejected with `400 Bad Request`.

```
curl "http://localhost:8080/shop/product/re2?nameFilter=^.*%5Beva%5D.*\$"
```

## Error responses

| Status | Cause |
| --- | --- |
| 400 | `nameFilter` missing, or not a valid pattern for the selected engine |
| 500 | Unexpected server error |

## Design notes

- Rows stream from Postgres to the HTTP response via a single reactive pipeline
  (`ProductRepository.streamAllAfter(afterId)` → `.filter(...)` → batched JSON encoding), so the
  full result set is never materialized in memory regardless of table size.
- The CPU-bound match evaluation runs on a dedicated `matcherScheduler`, off the Netty event-loop
  threads, so filtering on one request doesn't stall I/O for concurrent requests, and isolated from
  Reactor's shared global `Schedulers.parallel()` so it doesn't contend with unrelated CPU work.
- The JSON response is written in batches (`JsonArrayBodyWriter`, 500 rows per flush) rather than
  flushing once per row — at millions of rows, per-row flushing dominates response time far more
  than the database or payload size do.
- Each request compiles its own matcher from its own `nameFilter`; nothing mutable is shared across
  requests or across rows within a request.

## Scale testing

`src/main/resources/db/seed/seed-millions.sql` bulk-inserts 5,000,000 rows for manual load/memory
testing. It is not run automatically by the app, Flyway, or the test suite:

```
psql -h localhost -U shop -d shop -f src/main/resources/db/seed/seed-millions.sql
```

## Tests

```
./gradlew test
```

Requires Docker (integration tests use Testcontainers to run against real PostgreSQL).
