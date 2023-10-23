# monty

**monty** is a Java 21 library for estimating equity and expected value
in Texas hold 'em using Monte Carlo simulation.

Example usage:

```java
import static io.github.gdejohn.monty.Card.Rank.*;
import static io.github.gdejohn.monty.Card.Suit.*;
import static io.github.gdejohn.monty.Board.flop;
import static io.github.gdejohn.monty.Monty.equity;
import static io.github.gdejohn.monty.Monty.splits;
import static io.github.gdejohn.monty.Pocket.pocket;

class Demo {
    public static void main(String[] args) {
        var opponents = 3;
        var pocket = pocket(EIGHT.of(CLUBS), NINE.of(CLUBS));
        var board = flop(SEVEN.of(CLUBS), TEN.of(CLUBS), ACE.of(HEARTS));

        // an infinite, parallel, unordered stream of simulated games
        Stream<Integer> splits = splits(opponents, pocket, board);

        // an approximation of the fraction of the pot that would be
        // won on average across all possible outcomes
        BigDecimal equity = splits.limit(1_000_000).collect(equity());
        
        // precision tends to increase with more simulated games,
        // subject to diminishing returns
        assert Math.abs(equity.doubleValue() - 0.523d) < 0.001d;
    }
}
```
