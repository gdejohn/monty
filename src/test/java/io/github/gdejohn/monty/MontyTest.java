package io.github.gdejohn.monty;

import static io.github.gdejohn.monty.Card.board;
import static io.github.gdejohn.monty.Card.pocket;
import static io.github.gdejohn.monty.Card.Rank.ACE;
import static io.github.gdejohn.monty.Card.Rank.EIGHT;
import static io.github.gdejohn.monty.Card.Rank.FOUR;
import static io.github.gdejohn.monty.Card.Rank.KING;
import static io.github.gdejohn.monty.Card.Rank.NINE;
import static io.github.gdejohn.monty.Card.Rank.SEVEN;
import static io.github.gdejohn.monty.Card.Rank.TEN;
import static io.github.gdejohn.monty.Card.Suit.CLUBS;
import static io.github.gdejohn.monty.Card.Suit.HEARTS;
import static io.github.gdejohn.monty.Card.Suit.SPADES;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

class MontyTest {
    @Test
    void equity() {
        var seed = 0x702E9E5C611E3EB3L;
        var rng = new SplittableRandom(seed);
        var trials = 1L << 20;
        var opponents = 3;
        var pocket = pocket(EIGHT.of(CLUBS), NINE.of(CLUBS));
        var board = board(SEVEN.of(CLUBS), TEN.of(CLUBS), ACE.of(HEARTS));
        var simulation = Monty.simulate(rng, opponents, pocket, board);
        assertThat(simulation.limit(trials).collect(Monty.equity())).isEqualTo(0.5228184858957926d);
    }

    @Test
    void equitySequential() {
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
        assertThat(simulation.sequential().limit(trials).collect(Monty.equity())).isEqualTo(0.5222318967183431d);
    }

    @Test
    void flop() {
        var flop = board(FOUR.of(HEARTS), SEVEN.of(SPADES), KING.of(CLUBS));
        assertThat(flop.cards.length).isEqualTo(3);
        assertThat(flop.cards[0]).isEqualTo(0b10_0010_0000000000000_0000000000100_0000000000000_0000000000000L);
        assertThat(flop.cards[1]).isEqualTo(0b11_0101_0000000100000_0000000000000_0000000000000_0000000000000L);
        assertThat(flop.cards[2]).isEqualTo(0b00_1011_0000000000000_0000000000000_0000000000000_0100000000000L);
    }
}
