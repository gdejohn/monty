# monty

**monty** is a Java 21 library for estimating equity and expected value
in Texas hold 'em using Monte Carlo simulation.

```java
class Demo {
    void main() {
        var opponents = 3;
        var pocket = pocket(EIGHT.of(CLUBS), NINE.of(CLUBS));
        var board = flop(SEVEN.of(CLUBS), TEN.of(CLUBS), ACE.of(HEARTS));

        // an infinite, parallel, unordered stream of simulated games,
        // throughput scales linearly with number of threads
        Stream<Showdown> splits = splits(opponents, pocket, board);

        // custom collector returns object that can estimate equity
        // and expected value
        var monty = splits.limit(1_000_000).collect(monty());

        // an approximation of the fraction of the pot that would be
        // won on average across all possible outcomes
        BigDecimal equity = monty.equity();

        // precision tends to increase with additional simulated games,
        // subject to diminishing returns
        assert Math.abs(equity.doubleValue() - 0.5228d) < 0.001d;

        var raise = 50;
        var pot = 100;

        // an approximation of the ratio of expected winnings to the
        // size of a given raise
        BigDecimal expectedValue = monty.expectedValue(raise, pot);
    }
}
```
