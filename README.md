# monty

**monty** is a Java 21 library for calculating equity and expected value in Texas
hold 'em via parallel Monte Carlo simulation.

Example usage:

```java
import io.github.gdejohn.monty.Board;
import io.github.gdejohn.monty.Pocket;

import java.util.stream.Stream;

import static io.github.gdejohn.monty.Card.Rank.*;
import static io.github.gdejohn.monty.Card.Suit.*;
import static io.github.gdejohn.monty.Board.flop;
import static io.github.gdejohn.monty.Monty.equity;
import static io.github.gdejohn.monty.Monty.splits;
import static io.github.gdejohn.monty.Pocket.pocket;

class Demo {
    public static void main(String[] args) {
        int opponents = 3;
        Pocket pocket = pocket(EIGHT.of(CLUBS), NINE.of(CLUBS));
        Board board = flop(SEVEN.of(CLUBS), TEN.of(CLUBS), ACE.of(HEARTS));

        // An infinite, parallel, unordered stream of simulated games.
        Stream<Integer> splits = splits(opponents, pocket, board);

        // An approximation of the fraction of the pot that would be won on average
        // across all possible outcomes. Accuracy increases with more simulated games,
        // subject to diminishing returns.
        BigDecimal equity = splits.limit(1_000_000).collect(equity());
        
        assert Math.abs(equity.doubleValue() - 0.523d) <= 0.001d;
    }
}
```
