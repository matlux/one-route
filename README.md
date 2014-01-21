# one-route

A simple Clojure demonstrating the use of Ring, Compojure, HTML, JavaScript with Jquery to setup a minimalistic Ajax Webapp + matching Rest Services.

It contains 3 routes (or rest service entry points):

    GET     ->    /entry/:name
    PUT     ->    /entry
    DELETE  ->    /entry

They respectively get, add or delete a user of the system. There is no database. The data is a simple hashmap held inside an Atom.

## Usage

### Prerequisites:

 lein which you can install [here](https://github.com/technomancy/leiningen)

### Start repl:

    lein repl

### Start the server (in repl):

    (use 'one-route.core :reload)
    (def server (start-server))

### Stop the server (in repl):

    (.stop server)

By the way you don't need to restart the server when you change the code. Only repload the namespace with:

    (use 'one-route.core :reload)

## License

Copyright © 2014 Matlux

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
