CREATE TABLE _user
(
    id       UUID NOT NULL,
    username VARCHAR(255),
    password VARCHAR(255),
    role     VARCHAR(255),
    CONSTRAINT pk__user PRIMARY KEY (id)
);

CREATE TABLE track
(
    id        UUID NOT NULL,
    title     VARCHAR(255),
    artist    VARCHAR(255),
    album     VARCHAR(255),
    genre     VARCHAR(255),
    year      VARCHAR(255),
    file_name VARCHAR(255),
    CONSTRAINT pk_track PRIMARY KEY (id)
);

ALTER TABLE _user
    ADD CONSTRAINT uc__user_username UNIQUE (username);

ALTER TABLE track
    ADD CONSTRAINT uc_track_filename UNIQUE (file_name);

CREATE INDEX idx_user_username ON _user (username);
CREATE INDEX idx_track_title ON track (title);
CREATE INDEX idx_track_artist ON track (artist);
CREATE INDEX idx_track_album ON track (album);