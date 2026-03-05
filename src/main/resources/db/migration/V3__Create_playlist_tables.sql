CREATE TABLE playlist
(
    id      UUID         NOT NULL,
    name    VARCHAR(255) NOT NULL,
    user_id UUID         NOT NULL,
    CONSTRAINT pk_playlist PRIMARY KEY (id),
    CONSTRAINT fk_playlist_user FOREIGN KEY (user_id) REFERENCES _user (id) ON DELETE CASCADE
);

CREATE TABLE playlist_track
(
    playlist_id UUID    NOT NULL,
    track_id    UUID    NOT NULL,
    position    INTEGER NOT NULL,
    CONSTRAINT pk_playlist_track PRIMARY KEY (playlist_id, position),
    CONSTRAINT fk_pt_playlist FOREIGN KEY (playlist_id) REFERENCES playlist (id) ON DELETE CASCADE,
    CONSTRAINT fk_pt_track FOREIGN KEY (track_id) REFERENCES track (id) ON DELETE CASCADE
);

CREATE INDEX idx_playlist_user_id ON playlist (user_id);