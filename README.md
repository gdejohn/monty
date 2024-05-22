# monty

**monty** is a Java 22 library for estimating equity and expected value in
Texas hold 'em using Monte Carlo simulation.

```java
// tell monty your hole cards, the community cards, and the
// number of players including yourself
var monty = Monty.players(4)
                 .flop(SEVEN.of(CLUBS), TEN.of(CLUBS), ACE.of(HEARTS))
                 .pocket(EIGHT.of(CLUBS), NINE.of(CLUBS));

// get a lazy, infinite, parallel stream of simulated outcomes,
// throughput scales linearly with the number of threads
IntStream outcomes = monty.stream();

// or let monty run a given number of trials and summarize the
// outcomes for you
var showdown = monty.trials(1_000_000);

// your equity is the fraction of the pot that you won on
// average across every trial
BigDecimal equity = showdown.equity();

// precision tends to increase with more trials, subject to
// diminishing returns
assert Math.abs(equity.doubleValue() - 0.5228d) < 0.001d;

var raise = 50;
var pot = 100;

// the expected value of a call is the ratio of average
// winnings to the size of the raise
BigDecimal expectedValue = showdown.expectedValue(raise, pot);
```
