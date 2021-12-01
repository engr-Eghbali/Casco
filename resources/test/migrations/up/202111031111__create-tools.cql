CREATE TABLE IF NOT EXISTS tools (version varchar,
                                              checksum int,
                                              path varchar,
                                              applied_at timestamp,
                                              PRIMARY KEY (checksum));