CREATE TABLE IF NOT EXISTS tails (version varchar,
                                              checksum int,
                                              path varchar,
                                              applied_at timestamp,
                                              PRIMARY KEY (checksum));