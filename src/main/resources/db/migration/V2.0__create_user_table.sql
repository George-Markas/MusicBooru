CREATE TABLE _user
(
    id       UUID,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(5)   NOT NULL, -- 'USER', 'ADMIN,

    CONSTRAINT pk__user PRIMARY KEY (id),
    CONSTRAINT uc__user_username UNIQUE (username)
);