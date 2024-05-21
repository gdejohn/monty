# monty

**monty** is a Java 22 library for estimating equity and expected value in
Texas hold 'em using Monte Carlo simulation.

```java
void main() {
    var pocket = pocket(EIGHT.of(CLUBS), NINE.of(CLUBS));
    var board = flop(SEVEN.of(CLUBS), TEN.of(CLUBS), ACE.of(HEARTS));
    int opponents = 3;

    // a lazy, infinite, parallel stream of simulated outcomes
    IntStream showdown = Monty.showdown(pocket, board, opponents);

    // collect the winnings with a custom mutable reduction,
    // throughput scales linearly with the number of threads
    var monty = showdown.limit(1_000_000).collect(
        Monty::new,
        Monty::accumulate,
        Monty::combine
    );

    // the estimated equity is the average fraction of the pot
    // won by the player with the given hole cards
    BigDecimal equity = monty.equity();

    // precision tends to increase with more simulated games,
    // subject to diminishing returns
    assert Math.abs(equity.doubleValue() - 0.5228d) < 0.001d;

    var raise = 50;
    var pot = 100;

    // the expected value of a call is the ratio of estimated
    // winnings to the given raise
    BigDecimal expectedValue = monty.expectedValue(raise, pot);
}
```
