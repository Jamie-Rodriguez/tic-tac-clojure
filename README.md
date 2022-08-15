This is a game engine I've written for _tic-tac-toe_ AKA _noughts & crosses_ in Clojure, which is driven using _bitboards_.

I created this for two reasons:
1. To learn the modern, in-built Clojure development ecosystem, specifically using *CLI + Deps* with unit testing.
2. To use this engine/environment to experiment with reinforcement learning agents.


Usage
=====

Some code for initialising an example game is in the `main` function of the `engine` namespace (in `src/engine.clj`). This can be invoked via:

```shell
clj --main engine
```


Tests
=====

To run unit tests using [Cloverage](https://github.com/cloverage/cloverage), use the `test` alias:

```shell
clj -Mtest
```

Cloverage outputs high-level coverage data to the console, but then writes a more detailed HTML coverage report to `target/coverage/index.html` for closer examination.


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


Game AI
=======

I have created an artificial intelligence that can play competitively using the Monte Carlo tree search algorithm (MCTS).

The algorithm uses two main hyperparameters; *number of iterations* and the *exploration* parameter used in the upper confidence bound applied to trees, invented by Levente Kocsis and Csaba Szepesvári.

The *number of iterations* parameter behaves linearly with respect to performance - the more iterations that can be run, the greater the performance - as MCTS has been proven to converge to optimal play as *number of iterations* approaches infinity.

However the *exploration* parameter, used to balance the exploration - exploitation tradeoff, is not so simple. The value for optimal play would need to experimentally determined. I wanted to run some experiments to help picture what the function mapping `exploration -> performance` looks like.

I ran an experiment for a MCTS agent with *number of iterations* fixed at 1000 and varying the *exploration* parameter to search for the optimal value.

For each experimental run, the MCTS agent played 100,000 games against an agent that chooses completely random moves:

| exploration |  wins | losses | draws |
|:-----------:|:-----:|:------:|:-----:|
|     0.2     | 83936 |   785  | 15279 |
|     0.4     | 87555 |   409  | 12036 |
|     0.8     | 91494 |    95  |  8411 |
|     1.0     | 91494 |    43  |  8463 |
|     1.2     | 91709 |    28  |  8263 |
|     1.3     | 91575 |    41  |  8384 |
|     1.4     | 91664 |    35  |  8301 |
|     1.5     | 91503 |    41  |  8456 |
|     1.8     | 91577 |    66  |  8357 |
|     2.0     | 91638 |    54  |  8308 |
|     4.0     | 91235 |   111  |  8654 |
|     8.0     | 91162 |   271  |  8567 |

From the results, the values for *exploration* in the range `1.0 <= exploration <= 1.5` look the most promising.

I then decided to search that area with a higher granularity to see if there was a trend in performance:

| exploration |  wins | losses | draws |
|:-----------:|:-----:|:------:|:-----:|
|     1.00    | 91577 |   60   |  8363 |
|     1.05    | 91807 |   53   |  8140 |
|     1.10    | 91536 |   43   |  8421 |
|     1.15    | 91600 |   35   |  8365 |
|     1.20    | 91542 |   49   |  8409 |
|     1.25    | 91727 |   50   |  8223 |
|     1.30    | 91728 |   42   |  8230 |
|     1.35    | 91571 |   38   |  8391 |
|     1.40    | 91664 |   40   |  8296 |
|     1.45    | 91719 |   38   |  8243 |
|     1.50    | 91665 |   44   |  8291 |
|     1.55    | 91621 |   51   |  8328 |
|     1.60    | 91558 |   41   |  8401 |

Surprisingly there didn't seem to be much difference between the values. The difference between the best and worst win-percentage metric was less than 0.4%!

From these results the range `1.10 <= exploration <= 1.20` performed a little better than the rest, but further testing would be needed to confirm this.


To-Do
=====

To further develop an understanding of a typical Clojure project as well as property testing:
- Add specs for the functions and data structures used
- Add property testing
- Parallelise MCTS AI
