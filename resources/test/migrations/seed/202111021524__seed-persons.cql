BEGIN BATCH
INSERT INTO persons ("version", "checksum", "path", "applied_at")
VALUES (
            '202111021524',
            -1303363976,
            './resources/migrations/up/202111021524__init.cql',
            dateof(now())
      );

INSERT INTO persons ("version", "checksum", "path", "applied_at")
VALUES (
            '202111021525',
            -1303363977,
            './resources/migrations/up/202111021525__init.cql',
            dateof(now())
      );

INSERT INTO persons ("version", "checksum", "path", "applied_at")
VALUES (
            '202111021526',
            -1303363978,
            './resources/migrations/up/202111021526__init.cql',
            dateof(now())
      );
APPLY BATCH;