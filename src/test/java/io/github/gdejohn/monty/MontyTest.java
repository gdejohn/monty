package io.github.gdejohn.monty;

import org.junit.jupiter.api.Test;

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
import static org.assertj.core.api.Assertions.offset;

class MontyTest {
    @Test
    void equity() {
        var trials = 1L << 20;
        var opponents = 3;
        var simulation = Monty.simulate(
            opponents,
            pocket(EIGHT.of(CLUBS), NINE.of(CLUBS)),
            board(SEVEN.of(CLUBS), TEN.of(CLUBS), ACE.of(HEARTS))
        );
        assertThat(simulation.limit(trials).collect(Monty.equity())).isEqualTo(0.52279d, offset(0.005d));
    }
}
