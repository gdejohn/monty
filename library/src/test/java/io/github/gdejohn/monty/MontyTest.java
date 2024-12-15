package io.github.gdejohn.monty;

import io.github.gdejohn.monty.Deck.Generator;
import io.github.gdejohn.monty.Monty.Showdown;
import org.junit.jupiter.api.Test;

import static io.github.gdejohn.monty.Card.Rank.ACE;
import static io.github.gdejohn.monty.Card.Rank.EIGHT;
import static io.github.gdejohn.monty.Card.Rank.NINE;
import static io.github.gdejohn.monty.Card.Rank.SEVEN;
import static io.github.gdejohn.monty.Card.Rank.TEN;
import static io.github.gdejohn.monty.Card.Suit.CLUBS;
import static io.github.gdejohn.monty.Card.Suit.HEARTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class MontyTest {
    private static final byte[] seed = {
        +0x10, +0x2b, +0x38, -0x53,
        -0x12, -0x38, -0x6f, +0x1a,
        +0x51, +0x20, -0x52, +0x18,
        +0x7a, -0x28, -0x46, +0x02,
        +0x0e, +0x15, +0x56, +0x68,
        +0x08, -0x7c, +0x1e, -0x21,
        +0x7a, +0x5e, -0x32, -0x66,
        +0x6f, -0x3e, +0x3d, -0x78
    };

    private static Showdown showdown() {
        return Monty.pocket(EIGHT.of(CLUBS), NINE.of(CLUBS))
                    .flop(SEVEN.of(CLUBS), TEN.of(CLUBS), ACE.of(HEARTS))
                    .players(4)
                    .rng(new Generator(seed))
                    .limit(1 << 20);
    }

    @Test
    void equity() {
        assertThat(showdown().equity()).isCloseTo(0.5228d, within(0.0001d));
    }

    @Test
    void expectedValue() {
        int pot = 100;
        int raise = 50;
        assertThat(showdown().expectedValue(pot, raise)).isCloseTo(1.568d, within(0.001d));
    }
}
