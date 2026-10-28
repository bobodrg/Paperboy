# 0004. Plain SQL over JDBC for the backend database

- **Status:** accepted
- **Date:** 2026-10-28

## Context

The backend stores topics, sources, feeds and articles in PostgreSQL. The queries are few but specific: an idempotent upsert of articles from parallel feeds, and a topic filter with aggregated tags.

## Decision

Plain SQL with JDBC, a HikariCP connection pool and Flyway migrations (`backend/src/main/resources/db/migration`). No ORM. Every query lives in `JdbcNewsRepository`, behind the `NewsRepository` interface.

## Consequences

- The SQL that runs is the SQL in the code. PostgreSQL features (`ON CONFLICT … RETURNING`, `array_agg`, `ANY(?)`) are used directly.
- Result rows are mapped by hand, which is more verbose than with an ORM.
- JDBC blocks, so callers switch to `Dispatchers.IO`.
- `JdbcNewsRepositoryTest` runs against real PostgreSQL in Docker (Testcontainers), including a concurrency test.
- Revisit if the number of queries grows enough that the mapping code becomes a burden (candidates: Exposed, jOOQ).
