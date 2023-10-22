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
import static io.github.gdejohn.monty.Pocket.pocket;
import static org.assertj.core.api.Assertions.assertThat;


class MontyTest {
    private static Stream<Integer> splits() {
        var seed = 7590512158799335833L;
        var rng = new SplittableRandom(seed);
        var opponents = 3;
        var pocket = pocket(EIGHT.of(CLUBS), NINE.of(CLUBS));
        var board = flop(SEVEN.of(CLUBS), TEN.of(CLUBS), ACE.of(HEARTS));
        return Monty.splits(rng, opponents, pocket, board);
    }

    @Test
    void equity() {
        assertThat(splits().limit(1 << 20)).hasSize(1 << 20);
        assertThat(splits().limit(1 << 20).collect(Monty.equity())).hasToString("0.5228");
    }

    @Test
    void equitySequential() {
        assertThat(splits().limit(1 << 20).sequential()).hasSize(1 << 20);
        assertThat(splits().sequential().limit(1 << 20)).hasSize(1 << 20);
        assertThat(splits().limit(1 << 20).sequential().collect(Monty.equity())).hasToString("0.5228");
        assertThat(splits().sequential().limit(1 << 20).collect(Monty.equity())).hasToString("0.5228");
    }
}
