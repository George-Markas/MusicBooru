CREATE TABLE track
(
    id        BIGINT GENERATED ALWAYS AS IDENTITY,
    public_id CHAR(7)      NOT NULL,
    artist    VARCHAR(255),
    title     VARCHAR(255),
    album     VARCHAR(255),
    year      VARCHAR(10), -- The longest supported format is 'yyyy-MM-dd' e.g. 2010-11-22
    genre     VARCHAR(255),
    duration  INT,
    filename  VARCHAR(255) NOT NULL,
    mime_type VARCHAR(255) NOT NULL,

    CONSTRAINT pk_track PRIMARY KEY (id),
    CONSTRAINT uq_track_public_id UNIQUE (public_id),
    CONSTRAINT uq_track_filename UNIQUE (filename)
);