CREATE TABLE IF NOT EXISTS persons (version varchar,
                                              checksum int,
                                              path varchar,
                                              applied_at timestamp,
                                              PRIMARY KEY (checksum));