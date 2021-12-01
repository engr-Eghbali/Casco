CREATE TABLE IF NOT EXISTS books (version varchar,
                                              checksum int,
                                              path varchar,
                                              applied_at timestamp,
                                              PRIMARY KEY (checksum));