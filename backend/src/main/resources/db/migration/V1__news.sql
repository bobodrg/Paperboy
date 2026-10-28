CREATE TABLE topics (
    id        TEXT PRIMARY KEY,
    name      TEXT NOT NULL,
    parent_id TEXT REFERENCES topics (id)
);

CREATE TABLE sources (
    id           TEXT PRIMARY KEY,
    name         TEXT NOT NULL,
    homepage_url TEXT NOT NULL
);

CREATE TABLE feeds (
    url       TEXT PRIMARY KEY,
    source_id TEXT NOT NULL REFERENCES sources (id),
    topic_id  TEXT NOT NULL REFERENCES topics (id)
);

CREATE TABLE articles (
    id           BIGSERIAL PRIMARY KEY,
    url          TEXT        NOT NULL UNIQUE,
    title        TEXT        NOT NULL,
    summary      TEXT        NOT NULL,
    image_url    TEXT,
    source_id    TEXT        NOT NULL REFERENCES sources (id),
    published_at TIMESTAMPTZ NOT NULL,
    ingested_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX articles_published_at_idx ON articles (published_at DESC);

CREATE TABLE article_topics (
    article_id BIGINT NOT NULL REFERENCES articles (id) ON DELETE CASCADE,
    topic_id   TEXT   NOT NULL REFERENCES topics (id),
    PRIMARY KEY (article_id, topic_id)
);

CREATE INDEX article_topics_topic_idx ON article_topics (topic_id);
