CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_track_title_trgm ON track USING GIN (title gin_trgm_ops);
CREATE INDEX idx_track_artist_trgm ON track USING GIN (artist gin_trgm_ops);
CREATE INDEX idx_track_album_trgm ON track USING GIN (album gin_trgm_ops);