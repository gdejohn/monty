# eevee

**eevee** is a Java 11 library for calculating equity and expected value in Texas
hold 'em via parallel Monte Carlo simulation.

Example usage:

```java
import io.github.gdejohn.monty.Card.Board;
import io.github.gdejohn.monty.Card.Pocket;
import io.github.gdejohn.monty.Monty;
import io.github.gdejohn.monty.Monty.Winnings;

import java.util.stream.Stream;

import static io.github.gdejohn.monty.Card.Rank.*;
import static io.github.gdejohn.monty.Card.Suit.*;
import static io.github.gdejohn.monty.Card.board;
import static io.github.gdejohn.monty.Card.pocket;

class Demo {
    public static void main(String[] args) {
        int opponents = 3;
        Pocket pocket = pocket(EIGHT.of(CLUBS), NINE.of(CLUBS));
        Board board = board(SEVEN.of(CLUBS), TEN.of(CLUBS), ACE.of(HEARTS));

        /**
         * An infinite, parallel, unordered stream of winnings from simulated games.
         */
        Stream<Winnings> simulation = Monty.simulate(opponents, pocket, board);

        /**
         * An approximation of the fraction of the pot that you would win on average
         * across all possible outcomes. Accuracy increases with more simulated games,
         * subject to diminishing returns.
         */
        double equity = simulation.limit(1_000_000).collect(Monty.equity());
        
        assert Math.abs(equity - 0.52d) <= 0.01d;
    }
}
```
