package io.github.gdejohn.monty;

import io.github.gdejohn.monty.Monty.Preflop;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.DoubleStream;

import static io.github.gdejohn.monty.Card.Rank.ACE;
import static io.github.gdejohn.monty.Card.Rank.EIGHT;
import static io.github.gdejohn.monty.Card.Rank.NINE;
import static io.github.gdejohn.monty.Card.Rank.SEVEN;
import static io.github.gdejohn.monty.Card.Rank.TEN;
import static io.github.gdejohn.monty.Card.Suit.CLUBS;
import static io.github.gdejohn.monty.Card.Suit.HEARTS;
import static java.lang.Math.abs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

public class MontyTest {
    private static final double ERROR = 0.001d;

    private static final double CONFIDENCE = 0.999_999_999_999d;

    private static final double TRUE_EQUITY = 0.52279d;

    private static final double EXPECTED_VALUE = 1.5684d;

    private static final Monty MONTY =
        Monty.pocket(EIGHT.of(CLUBS), NINE.of(CLUBS))
             .flop(SEVEN.of(CLUBS), TEN.of(CLUBS), ACE.of(HEARTS))
             .players(4);

    @Test
    public void equity() {
        double equity = MONTY.equity(ERROR, CONFIDENCE).estimate();
        assertThat(equity).isCloseTo(TRUE_EQUITY, within(ERROR));
    }

    @Test
    public void expectedValue() {
        int pot = 100;
        int raise = 50;
        double expectedValue = MONTY.equity(ERROR, CONFIDENCE).expectedValue(pot, raise);
        assertThat(expectedValue).isCloseTo(EXPECTED_VALUE, within(0.01d));
    }

    @Test
    public void confidence() {
        double error = 0.01d;
        double confidence = 0.99d;
        long iterations = 10_000L;
        assertThat(
            DoubleStream.generate(() -> MONTY.equity(error, confidence).estimate())
                        .limit(iterations)
                        .filter(equity -> abs(equity - TRUE_EQUITY) < error)
                        .count()
        ).isGreaterThan((long) (iterations * confidence));
    }

    @Test
    public void tooManyTrials() {
        double error = 0.000_000_000_001d;
        assertThatThrownBy(
            () -> MONTY.equity(error, CONFIDENCE)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void duplicatePocketCards() {
        assertThatThrownBy(
            () -> Monty.pocket(EIGHT.of(CLUBS), EIGHT.of(CLUBS))
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void duplicateCard() {
        Preflop monty = Monty.pocket(EIGHT.of(CLUBS), NINE.of(CLUBS));
        assertThatThrownBy(
            () -> monty.flop(NINE.of(CLUBS), TEN.of(CLUBS), ACE.of(HEARTS))
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void duplicateCommunityCard() {
        Preflop monty = Monty.pocket(EIGHT.of(CLUBS), NINE.of(CLUBS));
        assertThatThrownBy(
            () -> monty.flop(TEN.of(CLUBS), TEN.of(CLUBS), ACE.of(HEARTS))
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1, 24})
    public void invalidPlayers(int players) {
        var monty = Monty.pocket(EIGHT.of(CLUBS), NINE.of(CLUBS));
        assertThatThrownBy(
            () -> monty.players(players)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void potLessThanRaise() {
        int pot = 50;
        int raise = 100;
        var equity = MONTY.equity();
        assertThatThrownBy(
            () -> equity.expectedValue(pot, raise)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void nonPositiveRaise() {
        int pot = 50;
        int raise = 0;
        var equity = MONTY.equity();
        assertThatThrownBy(
            () -> equity.expectedValue(pot, raise)
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
