# 0003. Start with publisher RSS feeds

- **Status:** accepted
- **Date:** 2026-10-25

## Context

Free tiers of commercial news APIs are mostly non-commercial or delayed. Paid tiers cost roughly $25–150 a month.

## Decision

The MVP ingests publishers' RSS/Atom feeds. Ingestion goes through a `NewsSource` interface so a paid API can be added later without changing the apps.

We show a story's headline, the publisher's own snippet, its image and a link to the original article. Summaries are only generated from text we are allowed to process.

## Consequences

- Free and real-time, with no vendor lock-in.
- Feeds vary in format and quality, and we maintain the feed list ourselves.
- No ready-made clustering or enrichment; we build it.
- Revisit when coverage or quality limits growth.
