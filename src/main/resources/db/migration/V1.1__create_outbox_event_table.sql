CREATE TABLE outbox_event
(
    id                BIGINT GENERATED ALWAYS AS IDENTITY,
    track_id          BIGINT     NOT NULL,
    track_public_id   bpchar(7)  NOT NULL,
    audio_path        VARCHAR    NOT NULL,
    artwork_path      VARCHAR,
    needs_transcoding BOOLEAN    NOT NULL DEFAULT FALSE,
    status            VARCHAR(7) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'DONE', 'FAILED'
    retries           INT        NOT NULL DEFAULT 0,
    created_at        TIMESTAMP  NOT NULL,

    CONSTRAINT pk_outbox_event PRIMARY KEY (id),
    CONSTRAINT uq_outbox_track_id UNIQUE (track_id),
    CONSTRAINT uq_outbox_track_public_id UNIQUE (track_public_id),
    CONSTRAINT fk_outbox_track FOREIGN KEY (track_id) REFERENCES track (id)
);