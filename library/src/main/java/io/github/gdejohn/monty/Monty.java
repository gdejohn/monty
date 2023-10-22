package io.github.gdejohn.monty;

import io.github.gdejohn.monty.Card.Cards;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Spliterator;
import java.util.SplittableRandom;
import java.util.function.Consumer;
import java.util.random.RandomGenerator.SplittableGenerator;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static io.github.gdejohn.monty.Deck.deck;
import static java.lang.Integer.signum;
import static java.math.RoundingMode.HALF_EVEN;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.summarizingLong;

public final class Monty {
    private static final MathContext DEFAULT_CONTEXT = new MathContext(4, HALF_EVEN);

    /**
     * Least common multiple of the integers from 1 to 23, inclusive.
     */
    private static final long LCM = 5_354_228_880L;

    private static final long[] shares = new long[24];

    static {
        for (var split = 1; split < shares.length; split++) {
            shares[split] = LCM / split;
        }
    }

    private Monty() {
        throw new AssertionError("this class is not intended to be instantiated");
    }

    public static Stream<Integer> splits(int opponents, Pocket pocket, Board board) {
        return splits(new SplittableRandom(), opponents, pocket, board);
    }

    public static Stream<Integer> splits(int opponents, Pocket pocket) {
        return splits(opponents, pocket, Board.PRE_FLOP);
    }

    static Stream<Integer> splits(SplittableGenerator rng, int opponents, Pocket pocket, Board board) {
        class Splits implements Spliterator<Integer> {
            private final Deck deck;

            private final Pocket pocket;

            private final Hand partial;

            private long trials;

            Splits(Deck deck, Pocket pocket, Hand partial, long trials) {
                this.deck = deck;
                this.pocket = pocket;
                this.partial = partial;
                this.trials = trials;
            }

            @Override
            public int characteristics() {
                return IMMUTABLE | NONNULL | SIZED | SUBSIZED;
            }

            @Override
            public long estimateSize() {
                return trials;
            }

            @Override
            public Splits trySplit() {
                if (trials < 2) {
                    return null;
                } else {
                    return new Splits(
                        deck.split(),
                        pocket,
                        partial,
                        trials - (trials >>>= 1)
                    );
                }
            }

            @Override
            public boolean tryAdvance(Consumer<? super Integer> action) {
                if (trials < 1) {
                    return false;
                } else {
                    trials--;
                    deck.shuffle();
                    var board = deck.deal(partial, deck.size() - 45);
                    var value = pocket.evaluate(board);
                    var split = 1;
                    for (var n = 0; n < opponents; n++) {
                        switch (signum(value - deck.deal(board, 2).evaluate())) {
                            case 0: split++;
                            case 1: continue;
                        }
                        split = 0;
                        break;
                    }
                    action.accept(split);
                    return true;
                }
            }
        }

        if (opponents < 1 || opponents > 22) {
            throw new IllegalArgumentException(
                STR."opponents = \{opponents} (must be greater than 0 and less than 23)"
            );
        } else if ((pocket.pack() & board.pack()) != 0) {
            throw new IllegalArgumentException(
                STR."""
                    pocket \{pocket} and board \{board} must be disjoint, but both contain
                    \{Cards.toString(Cards.unpack(pocket.pack() & board.pack()))}
                """
            );
        } else {
            return StreamSupport.stream(
                new Splits(
                    deck(board, pocket, rng),
                    pocket,
                    board.hand(),
                    Long.MAX_VALUE
                ),
                true // parallel
            );
        }
    }

    public static Collector<Integer, ?, BigDecimal> equity() {
        return equity(DEFAULT_CONTEXT);
    }

    public static Collector<Integer, ?, BigDecimal> equity(MathContext context) {
        return collectingAndThen(
            summarizingLong(split -> shares[split]),
            stats -> BigDecimal.valueOf(stats.getSum()).divide(
                BigDecimal.valueOf(LCM).multiply(BigDecimal.valueOf(stats.getCount())),
                context
            )
        );
    }

    public static Collector<Integer, ?, BigDecimal> expectedValue(long pot, long raise) {
        return expectedValue(pot, raise, DEFAULT_CONTEXT);
    }

    public static Collector<Integer, ?, BigDecimal> expectedValue(long pot, long raise, MathContext context) {
        if (pot <= 0.0d) {
            throw new IllegalArgumentException(STR."pot = \{pot} (must be positive)");
        } else if (raise <= 0.0d) {
            throw new IllegalArgumentException(STR."raise = \{raise} (must be positive)");
        } else {
            return expectedValue(BigDecimal.valueOf(pot), BigDecimal.valueOf(raise), context);
        }
    }

    private static Collector<Integer, ?, BigDecimal> expectedValue(BigDecimal pot, BigDecimal raise, MathContext context) {
        return collectingAndThen(
            summarizingLong(split -> shares[split]),
            stats -> BigDecimal.valueOf(stats.getSum()).multiply(pot.add(raise)).divide(
                BigDecimal.valueOf(stats.getCount()).multiply(raise).multiply(BigDecimal.valueOf(LCM)),
                context
            )
        );
    }
}
