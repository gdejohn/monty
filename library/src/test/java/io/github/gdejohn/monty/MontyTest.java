package io.github.gdejohn.monty;

import io.github.gdejohn.monty.Deck.Generator;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static io.github.gdejohn.monty.Board.flop;
import static io.github.gdejohn.monty.Card.Rank.ACE;
import static io.github.gdejohn.monty.Card.Rank.EIGHT;
import static io.github.gdejohn.monty.Card.Rank.NINE;
import static io.github.gdejohn.monty.Card.Rank.SEVEN;
import static io.github.gdejohn.monty.Card.Rank.TEN;
import static io.github.gdejohn.monty.Card.Suit.CLUBS;
import static io.github.gdejohn.monty.Card.Suit.HEARTS;
import static io.github.gdejohn.monty.Pocket.pocket;
import static org.assertj.core.api.Assertions.assertThat;

class MontyTest {
    private static final long trials = 1L << 20,
                               raise = 50,
                                 pot = 100;

    private static final String equity = "0.5228",
                                 value = "1.568";

    private static final byte[] seed = {
        +0x10, +0x2B, +0x38, -0x53,
        -0x12, -0x38, -0x6F, +0x1A,
        +0x51, +0x20, -0x52, +0x18,
        +0x7A, -0x28, -0x46, +0x02,
        +0x0E, +0x15, +0x56, +0x68,
        +0x08, -0x7C, +0x1E, -0x21,
        +0x7A, +0x5E, -0x32, -0x66,
        +0x6F, -0x3E, +0x3D, -0x78
    };

    private static IntStream showdown() {
        var pocket = pocket(EIGHT.of(CLUBS), NINE.of(CLUBS));
        var board = flop(SEVEN.of(CLUBS), TEN.of(CLUBS), ACE.of(HEARTS));
        var opponents = 3;
        var rng = new Generator(seed);
        return Monty.showdown(pocket, board, opponents, rng);
    }

    @Test
    void parallel() {
        var monty = showdown().limit(trials).collect(
            Monty::new,
            Monty::accumulate,
            Monty::combine
        );
        assertThat(monty.equity()).hasToString(equity);
        assertThat(monty.expectedValue(raise, pot)).hasToString(value);
    }

    @Test
    void sequential() {
        var monty = showdown().sequential().limit(trials).collect(
            Monty::new,
            Monty::accumulate,
            Monty::combine
        );
        assertThat(monty.equity()).hasToString(equity);
        assertThat(monty.expectedValue(raise, pot)).hasToString(value);
    }
}
