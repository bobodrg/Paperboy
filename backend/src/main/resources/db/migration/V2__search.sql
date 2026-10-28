-- Full-text search over headlines (weight A) and summaries (weight B), kept up to date by PostgreSQL.
ALTER TABLE articles
    ADD COLUMN search tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(summary, '')), 'B')
    ) STORED;

CREATE INDEX articles_search_idx ON articles USING GIN (search);
