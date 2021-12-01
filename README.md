# Casco

**Is just a simple, dev-env friendly tool to help you intract with states of your cassandra dbs.**

## Operations
**Up** can migrate-up your tables from versioned migration files. (from /up dir)
**Down** let you revert the state(s) like droping created tables from same versioned files. (from /down dir)
**Seed** you put seeds there and your dbs will grow! it's useful for filling tables. (from /seed dir)
**forget** casco will just purge your migration history map, includes checksums, versions, last applied, etc.


## Directories

Casco need a migration source directory containing at least one of /up , /down , /seed , or even all of them.
The default source location is ./resources/migrationss but its also configurable in casco config file we'll see further.
The default directory of casco is root (./casco.edn) but this is configurable too.

## Profiles
This concept let you do the separation on your opertations both logically and technically. The first level keys in casco config file indicates to a profile you name it like dev, test , trans, etc.
Each profile key can contains a unique map of migration settigs we'll be discused further.

    {:dev  {:src  "./resources/dev/migrations"
            :dbs  [{:contact-points ["localhost:9042"]
                    :session-keyspace "dev"
                    :load-balancing-local-datacenter "datacenter1"
                    :auto-create    true}]
            :mode :rebase}
    
     :test {:src  "./resources/test/migrations"
            :dbs  [{:contact-points ["localhost:9042"]
                    :session-keyspace "test"}]
            :mode :force}
			}

##Conflict strategy modes
Mode can help you face with the conflict situations like checksum colision or version-order inconsistency.
**-- Strict:** If there is conflict, then it will not step further.
**-- Force:** The conflicts are not considered at all.
**-- Rebase:** This strategy will try to  do the "down" operation from first conflict to the end and try to build them up again.

##Miscellaneous
**-- Auto-create:** The db configuration map is just like mpenet/alia cause we used it! but casco supports an extra property which will try to create the keyspace you provide if not exist.
##Command Examples

>lein casco up
>lein casco up --force
>lein casco up :test --strict
>lein casco up :test --strict  "./my-confs/casco.edn"

>**lein casco <operation> <:profile-key> <--mode>  < config/file/path>**

>lein casco down
>lein casco down --force
>lein casco down :dev --force
>lein casco down :dev --force "./my-confs/casco.edn"

---------------------------------------------------------

>lein casco seed :dev
>lein casco seed :dev "./my-confs/casco.edn"

----------------------------------------------------------
>lein casco forget :dev
>lein casco forget :dev "./my-confs/casco.edn"
