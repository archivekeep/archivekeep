CREATE TABLE IF NOT EXISTS "user"
(
    "id"       INTEGER NOT NULL,
    "email"    VARCHAR(255) NOT NULL,
    "password" VARCHAR(255) NOT NULL,

    PRIMARY KEY ("id" AUTOINCREMENT)
);

CREATE TABLE IF NOT EXISTS "session"
(
    "id"      INTEGER NOT NULL,
    "token"   VARCHAR(255) NOT NULL,
    "user_id" VARCHAR(255) NOT NULL,

    PRIMARY KEY ("id" AUTOINCREMENT)
);

CREATE TABLE IF NOT EXISTS "archive"
(
    "id"    INTEGER NOT NULL,
    "type"  VARCHAR(255) NOT NULL,
    "name"  VARCHAR(255) NOT NULL,
    "owner" VARCHAR(255) NOT NULL,

    PRIMARY KEY ("id" AUTOINCREMENT)
);

CREATE TABLE IF NOT EXISTS "personal_access_token"
(
    "id"                INTEGER NOT NULL,
    "user_id"           INTEGER NOT NULL,
    "name"              VARCHAR(255) NOT NULL,
    "token_salt"        VARCHAR(30) NOT NULL,
    "token_hash"        VARCHAR(30) NOT NULL,
    "token_last_eight"  CHAR(8) NOT NULL,

    PRIMARY KEY ("id" AUTOINCREMENT),
    UNIQUE ("user_id", "name")
)

/* TODO: ALTER TABLE user_access_token - add timestamps for expiration */
