package io.github.gdejohn.monty;

import java.math.BigDecimal;

import static java.math.MathContext.UNLIMITED;

public sealed interface Showdown {
    /**
     * Least common multiple of the integers from 1 to 23, inclusive.
     */
    static final BigDecimal LCM = BigDecimal.valueOf(5_354_228_880L);

    static final long[] winnings = winnings();

    private static long[] winnings() {
        var winnings = new long[24];
        for (var split = 1; split < winnings.length; split++) {
            winnings[split] = LCM.divide(BigDecimal.valueOf(split), UNLIMITED).longValueExact();
        }
        return winnings;
    }

    public record Win() implements Showdown {}

    public record Loss() implements Showdown {}

    public record Tie(int split) implements Showdown {
        public Tie {
            if (split < 2 || split > 23) {
                throw new IllegalArgumentException();
            }
        }
    }

    static final Win WIN = new Win();

    static final Loss LOSS = new Loss();

    static Showdown showdown(int split) {
        return switch (split) {
            case 1 -> WIN;
            case 0 -> LOSS;
            default -> new Tie(split);
        };
    }

    static long winnings(Showdown showdown) {
        return switch (showdown) {
            case Tie(var split) -> winnings[split];
            case Win() -> winnings[1];
            case Loss() -> winnings[0];
        };
    }
}
