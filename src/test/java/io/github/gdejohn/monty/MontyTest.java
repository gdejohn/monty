package io.github.gdejohn.monty;

import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

import static io.github.gdejohn.monty.Card.Rank.ACE;
import static io.github.gdejohn.monty.Card.Rank.EIGHT;
import static io.github.gdejohn.monty.Card.Rank.NINE;
import static io.github.gdejohn.monty.Card.Rank.SEVEN;
import static io.github.gdejohn.monty.Card.Rank.TEN;
import static io.github.gdejohn.monty.Card.Suit.CLUBS;
import static io.github.gdejohn.monty.Card.Suit.HEARTS;
import static io.github.gdejohn.monty.Card.board;
import static io.github.gdejohn.monty.Card.pocket;
import static org.assertj.core.api.Assertions.assertThat;

class MontyTest {
    @Test
    void equity() {
        var seed = 0x702E9E5C611E3EB3L;
        var generator = new SplittableRandom(seed);
        var trials = 1L << 20;
        var opponents = 3;
        var simulation = Monty.simulate(
            generator,
            opponents,
            pocket(EIGHT.of(CLUBS), NINE.of(CLUBS)),
            board(SEVEN.of(CLUBS), TEN.of(CLUBS), ACE.of(HEARTS))
        );
        assertThat(simulation.limit(trials).collect(Monty.equity())).isEqualTo(0.5228169049456032d);
    }
}
