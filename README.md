# monty

**monty** is a Java 22 library for estimating equity and expected value in
Texas hold 'em using Monte Carlo simulation.

### Usage

Tell monty your hole cards, the community cards, and the number of players including yourself:

```java
var monty = Monty.pocket(EIGHT.of(CLUBS), NINE.of(CLUBS))
                 .flop(SEVEN.of(CLUBS), TEN.of(CLUBS), ACE.of(HEARTS))
                 .players(4);
```

Get a lazy, infinite, parallel stream that pseudorandomly samples the possible outcomes given
those constraints, running independent simulations on every thread made available to it:

```java
IntStream outcomes = monty.stream();
```

An outcome is represented by a nonnegative integer indicating the number of players that you
split the pot with, including yourself: 0 means you lost, 1 means you won, and n > 1 means an
n-way tie. Or monty can run a given number of trials and summarize the outcomes for you:

```java
var showdown = monty.limit(1_000_000);
```

Your equity is the fraction of the pot that you won on average across every simulated outcome,
which converges to the true average across every possible outcome:

```java
assert Math.abs(showdown.equity() - 0.5228d) < 0.001d;
```

The expected value of a call is the ratio of your estimated winnings to the size of the raise:

```java
var pot = 100;
var raise = 50;

assert showdown.expectedValue(pot, raise) > 1.0d;
```

The increase in precision obtained by further trials is subject to diminishing returns.
