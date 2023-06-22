# org.pilosus/next-jdbc-test

An example of Clojure app with:

 - [next.jdbc](https://cljdoc.org/d/seancorfield/next.jdbc/) library for JDBC-based access to a PostgreSQL database
 - Connection pool with [HikariCP](https://github.com/brettwooldridge/HikariCP)
 - Database connection managed with the [mount](https://github.com/tolitius/mount) library (system state/dependency injection management)
 - Tests fixtures with transaction rollbacks

The app is for educational purposes. Mostly to show how to deal with the unit tests fixtures to rollback nested transactions in `next.jdbc`
on the test tear down. Please refer to the library [docs](https://cljdoc.org/d/seancorfield/next.jdbc/1.2.659/doc/getting-started/transactions) for more details. This may be of help when migrating from [clojure.java.jdbc](https://github.com/clojure/java.jdbc) to `next.jdbc`. See also the [migration guide](https://cljdoc.org/d/seancorfield/next.jdbc/1.2.659/doc/migration-from-clojure-java-jdbc).

## Requirements

- [Clojure](https://clojure.org/guides/install_clojure)
- [Docker Compose](https://docs.docker.com/compose/install/)

## Usage

Start an instance of PostgresSQL database:

    $ make up  # or docker-compose up

Download dependencies:

    $ make deps  # or clojure -X:deps prep

Run the tests:

    $ make test  # clojure -X:test


## License

Copyright (c) 2023 Vitaly Samigullin and contributors. All rights reserved.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.
