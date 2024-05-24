# monty

**monty** is a Java 22 library for estimating equity and expected value in
Texas hold 'em using Monte Carlo simulation.

### Usage

Tell monty your hole cards, the community cards, and the number of players including yourself:
```java
var monty = Monty.players(4)
                 .flop(SEVEN.of(CLUBS), TEN.of(CLUBS), ACE.of(HEARTS))
                 .pocket(EIGHT.of(CLUBS), NINE.of(CLUBS));
```

Get a lazy, infinite, parallel stream of simulated outcomes:
```java
IntStream outcomes = monty.stream();
```
The outcome of a trial is represented by a nonnegative integer indicating the number of players
that you split the pot with, including yourself: 0 means you lost, 1 means you won, and n > 1
means an n-way tie. Throughput scales linearly with the number of threads.

Or monty can run a given number of trials and summarize the outcomes for you:
```java
var showdown = monty.trials(1_000_000);
```

Your equity is the fraction of the pot that you won on average across every trial. Precision
tends to increase with more trials, subject to diminishing returns:
```java
assert Math.abs(showdown.equity() - 0.5228d) < 0.001d;
```

The expected value of a call is the ratio of average winnings to the size of the raise:
```java
var pot = 100;
var raise = 50;

assert showdown.expectedValue(pot, raise) > 1.0d;
```
