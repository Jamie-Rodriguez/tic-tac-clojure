This repo is a bit of an experiment I'm testing on myself.

I've written a basic game engine for tic-tac-toe in Clojure, which is driven using bitboards. Part of this experiment is to develop a better understand of using bitboards and efficient bitwise operations.

The experiment is for me to review this code in a few months and see if I still understand what I had written. This is a litmus test of sorts to see how clearly I write code, particularly in a language that I am very new to.


## Usage

Some code for initialising an example game is in the `main` function of the `engine` namespace (in `src/engine.clj`). This can be invoked via:

```shell
clj --main engine
```


## Tests

To run the Cognitect Labs test runner using the `test` alias:

```shell
clj -Mtest
```

## Building a Standalone `.jar` File

Build a standalone *uberjar* with the following commands:

```shell
clj -T:build clean
clj -T:build uber
```

From there you can execute the compiled `.jar` with

```shell
java -jar target/tic-tac-clojure-1.0.11-standalone.jar
```

## To-Do

To further develop an understanding of a typical Clojure project as well as property testing:
- Add spec for the namespace `engine`.
- Get around to the to-do's in `src/engine.clj`
- Add property testing, *especially* to the random move generation function `random-valid-move`