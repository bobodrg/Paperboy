# 0002. Our own backend between the apps and news providers

- **Status:** accepted
- **Date:** 2026-10-25

## Context

The apps need news from several providers, deduplicated, grouped by story and ranked per user.

## Decision

A Kotlin/Ktor backend ingests news, deduplicates and clusters it, and serves each user's brief. The apps only call our API.

## Consequences

- No provider API keys inside the app binary, where they can't be kept secret.
- Provider quotas aren't shared by every installed app.
- Deduplication, clustering and summaries run once per article instead of on every phone.
- Providers can be swapped without an app release.
- We pay for hosting and run one more system.
