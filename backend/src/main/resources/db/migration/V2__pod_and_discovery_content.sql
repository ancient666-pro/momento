-- Momento: Pod ecosystem, editorial content, and media URL support

-- Direct playable/downloadable URL for an uploaded object (CDN or public bucket URL)
ALTER TABLE media_assets ADD COLUMN url TEXT;
ALTER TABLE media_assets ADD COLUMN width INT;
ALTER TABLE media_assets ADD COLUMN height INT;

-- Story metadata used by the story viewer
ALTER TABLE stories ADD COLUMN caption TEXT NOT NULL DEFAULT '';
ALTER TABLE stories ADD COLUMN live_viewer_count BIGINT NOT NULL DEFAULT 0;

-- Moments (short-form creator videos)
ALTER TABLE moments ADD COLUMN thumbnail_url TEXT;
ALTER TABLE moments ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0;

-- Profiles: optional, self-declared age used by opt-in nearby discovery
ALTER TABLE user_profiles ADD COLUMN age INT;

-- Pods: live session flags + artwork
ALTER TABLE pods ADD COLUMN artwork_url TEXT;
ALTER TABLE pods ADD COLUMN live BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE pods ADD COLUMN listener_count BIGINT NOT NULL DEFAULT 0;
ALTER TABLE pods ADD COLUMN audio_only BOOLEAN NOT NULL DEFAULT TRUE;

-- Poll discussion counters
ALTER TABLE pod_polls ADD COLUMN reply_count BIGINT NOT NULL DEFAULT 0;

-- Posts: content kind + optional hashtag badge for Pod media reels
ALTER TABLE posts ADD COLUMN kind TEXT NOT NULL DEFAULT 'post';
ALTER TABLE posts ADD COLUMN hashtag TEXT;

-- Optional coordinates accepted from opted-in clients; PostGIS geography is derived
-- from these in spatial queries. Exact coordinates are never returned to other users.
ALTER TABLE user_location_data ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE user_location_data ADD COLUMN longitude DOUBLE PRECISION;

-- Trending Pod topics (hashtags with live discussion counters)
CREATE TABLE pod_topics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL UNIQUE,
    participant_count BIGINT NOT NULL DEFAULT 0,
    live BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Editorial dispatch cards shown in the Pod feed
CREATE TABLE editorial_dispatches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    snippet TEXT NOT NULL,
    domain TEXT NOT NULL,
    author_name TEXT NOT NULL,
    read_minutes INT NOT NULL DEFAULT 5,
    footnote_count BIGINT NOT NULL DEFAULT 0,
    participant_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Community soundtrack cards
CREATE TABLE soundtracks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    artist TEXT NOT NULL,
    artwork_url TEXT NOT NULL,
    rank INT NOT NULL DEFAULT 1,
    clip_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Long-form creator essays discussed in Pod
CREATE TABLE essays (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    preview TEXT NOT NULL DEFAULT '',
    like_count BIGINT NOT NULL DEFAULT 0,
    comment_count BIGINT NOT NULL DEFAULT 0,
    share_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_essays_created ON essays (created_at DESC);

-- Live audio rooms are Pods; sessions track real join state.
CREATE INDEX idx_pod_sessions_open ON pod_sessions (pod_id) WHERE ended_at IS NULL;
