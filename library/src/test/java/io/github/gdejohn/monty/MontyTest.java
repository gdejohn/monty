package io.github.gdejohn.monty;

import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;
import java.util.stream.Stream;

import static io.github.gdejohn.monty.Board.flop;
import static io.github.gdejohn.monty.Card.Rank.ACE;
import static io.github.gdejohn.monty.Card.Rank.EIGHT;
import static io.github.gdejohn.monty.Card.Rank.NINE;
import static io.github.gdejohn.monty.Card.Rank.SEVEN;
import static io.github.gdejohn.monty.Card.Rank.TEN;
import static io.github.gdejohn.monty.Card.Suit.CLUBS;
import static io.github.gdejohn.monty.Card.Suit.HEARTS;
import static io.github.gdejohn.monty.Monty.monty;
import static io.github.gdejohn.monty.Pocket.pocket;
import static org.assertj.core.api.Assertions.assertThat;


class MontyTest {
    private static final long trials = 1L << 20;

    private static Stream<Showdown> splits() {
        var seed = 7590512158799335833L;
        var rng = new SplittableRandom(seed);
        var opponents = 3;
        var pocket = pocket(EIGHT.of(CLUBS), NINE.of(CLUBS));
        var board = flop(SEVEN.of(CLUBS), TEN.of(CLUBS), ACE.of(HEARTS));
        return Monty.splits(rng, opponents, pocket, board);
    }

    @Test
    void equity() {
        var monty = splits().limit(trials).collect(monty());
        assertThat(monty.trials()).isEqualTo(trials);
        assertThat(monty.equity()).hasToString("0.5228");
    }

    @Test
    void equitySequential() {
        var splits = splits().sequential().limit(trials).collect(monty());
        assertThat(splits.trials()).isEqualTo(trials);
        assertThat(splits.equity()).hasToString("0.5228");
    }
}
