# monty

**monty** is a Java 22 library for estimating equity and expected value in
Texas hold 'em using Monte Carlo simulation.

```java
var pocket = pocket(EIGHT.of(CLUBS), NINE.of(CLUBS));
var board = flop(SEVEN.of(CLUBS), TEN.of(CLUBS), ACE.of(HEARTS));
int players = 4;
int trials = 1_000_000;

// tell monty the hole cards, the community cards, and the
// number of players
var monty = new Monty(pocket, board, players);

// a lazy, infinite, parallel stream of simulated outcomes,
// throughput scales linearly with the number of threads
IntStream outcomes = monty.stream();

// or summarize the outcomes
var showdown = monty.showdown(trials);

// and report the average fraction of the pot won by the
// player with the hole cards
BigDecimal equity = showdown.equity();

// precision tends to increase with more simulated games,
// subject to diminishing returns
assert Math.abs(equity.doubleValue() - 0.5228d) < 0.001d;

var raise = 50;
var pot = 100;

// the expected value of a call is the ratio of average
// winnings to a given raise
BigDecimal expectedValue = showdown.expectedValue(raise, pot);
```
