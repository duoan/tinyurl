CREATE TABLE t_url_mappings
(
    short_url  VARCHAR(9)    NOT NULL,
    long_url   VARCHAR(2048) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    CONSTRAINT pk_t_url_mappings PRIMARY KEY (short_url)
);

ALTER TABLE t_url_mappings
    ADD CONSTRAINT uc_t_url_mappings_long_url UNIQUE (long_url);