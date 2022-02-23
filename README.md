This is a game engine I've written for _tic-tac-toe_ AKA _noughts & crosses_ in Clojure, which is driven using _bitboards_.

I created this for two reasons:
1. To learn the modern, in-built Clojure development ecosystem, specifically using *CLI + Deps* with unit testing.
2. To use this engine/environment in the future to experiment with reinforcement learning agents.


Usage
=====

Some code for initialising an example game is in the `main` function of the `engine` namespace (in `src/engine.clj`). This can be invoked via:

```shell
clj --main engine
```


Tests
=====

To run the Cognitect Labs test runner using the `test` alias:

```shell
clj -Mtest
```


Building a Standalone `.jar` File
=================================

Build a standalone *uberjar* with the following commands:

```shell
clj -T:build clean
clj -T:build uber
```

From there you can execute the compiled `.jar` with

```shell
java -jar target/tic-tac-clojure-1.0.11-standalone.jar
```


To-Do
=====

To further develop an understanding of a typical Clojure project as well as property testing:
- Add spec for the namespace `engine`.
- Get around to the to-do's in `src/engine.clj`
- Add property testing, *especially* to the random move generation function `random-valid-move`
