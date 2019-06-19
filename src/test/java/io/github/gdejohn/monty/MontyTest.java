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
import static io.github.gdejohn.monty.Monty.board;
import static io.github.gdejohn.monty.Monty.pocket;
import static org.assertj.core.api.Assertions.assertThat;

class MontyTest {
    @Test
    void equity() {
        var opponents = 3;
        var trials = 1 << 20;
        var seed = 0x702E9E5C611E3EB3L;
        var simulation = Monty.simulation(
            opponents,
            pocket(EIGHT.of(CLUBS), NINE.of(CLUBS)),
            board(SEVEN.of(CLUBS), TEN.of(CLUBS), ACE.of(HEARTS)),
            trials,
            new SplittableRandom(seed)
        );
        assertThat(simulation.equity()).isEqualTo(0.5235997835795084d);
    }
}
