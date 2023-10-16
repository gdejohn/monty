package io.github.gdejohn.monty;

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

import java.util.SplittableRandom;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.github.gdejohn.monty.Monty.Showdown;

class MontyTest {
    private static Stream<Showdown> outsideStraightFlushDraw() {
        var seed = 8736237757166344667L;
        var rng = new SplittableRandom(seed);
        var trials = 1 << 20;
        var opponents = 3;
        var pocket = pocket(EIGHT.of(CLUBS), NINE.of(CLUBS));
        var board = flop(SEVEN.of(CLUBS), TEN.of(CLUBS), ACE.of(HEARTS));
        return Monty.simulate(rng, opponents, trials, pocket, board);
    }

    @Test
    void equity() {
        assertThat(outsideStraightFlushDraw().collect(Monty.equity())).hasToString("0.5228");
    }

    @Test
    void equitySequential() {
        assertThat(outsideStraightFlushDraw().sequential().collect(Monty.equity())).hasToString("0.5228");
    }
}
