package io.github.gdejohn.monty;

import io.github.gdejohn.monty.Card.Cards;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.LongSummaryStatistics;
import java.util.Spliterator;
import java.util.SplittableRandom;
import java.util.function.Consumer;
import java.util.random.RandomGenerator.SplittableGenerator;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static io.github.gdejohn.monty.Deck.deck;
import static io.github.gdejohn.monty.Showdown.LOSS;
import static io.github.gdejohn.monty.Showdown.WIN;
import static io.github.gdejohn.monty.Showdown.nextSplit;
import static java.lang.Integer.signum;
import static java.math.RoundingMode.HALF_EVEN;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.summarizingLong;

public final class Monty {
    private static final BigDecimal LCM = BigDecimal.valueOf(5_354_228_880L);

    private static final long[] shares = new long[24];

    static {
        for (var split = 1; split < shares.length; split++) {
            shares[split] = LCM.longValueExact() / split;
        }
    }

    private static final MathContext DEFAULT_CONTEXT = new MathContext(4, HALF_EVEN);

    final BigDecimal share;
    final BigDecimal trials;

    private Monty(LongSummaryStatistics stats) {
        this.share = BigDecimal.valueOf(stats.getSum());
        this.trials = BigDecimal.valueOf(stats.getCount());
    }

    public static Collector<Showdown, ?, Monty> monty() {
        return collectingAndThen(
            summarizingLong(showdown -> shares[showdown.split()]),
            Monty::new
        );
    }

    public BigDecimal equity() {
        return equity(DEFAULT_CONTEXT);
    }

    public BigDecimal equity(MathContext context) {
        return share.divide(trials.multiply(LCM), context);
    }

    private BigDecimal expectedValue(BigDecimal raise, BigDecimal pot, MathContext context) {
        return share.multiply(pot.add(raise)).divide(
            trials.multiply(LCM).multiply(raise),
            context
        );
    }

    public BigDecimal expectedValue(long raise, long pot, MathContext context) {
        if (pot <= 0.0d) {
            throw new IllegalArgumentException(STR."pot = \{pot} (must be positive)");
        } else if (raise <= 0.0d) {
            throw new IllegalArgumentException(STR."raise = \{raise} (must be positive)");
        } else {
            return expectedValue(BigDecimal.valueOf(raise), BigDecimal.valueOf(pot), context);
        }
    }

    /**
     * The expected value of calling a given raise.
     *
     * <p>Note that the {@code pot} includes the raise, but not the call.
     *
     * @param raise the raise that a player is deciding whether to call
     * @param pot   the size of the pot
     */
    public BigDecimal expectedValue(long raise, long pot) {
        return expectedValue(raise, pot, DEFAULT_CONTEXT);
    }

    public static Stream<Showdown> splits(int opponents, Pocket pocket, Board board) {
        return splits(new SplittableRandom(), opponents, pocket, board);
    }

    public static Stream<Showdown> splits(int opponents, Pocket pocket) {
        return splits(opponents, pocket, Board.PRE_FLOP);
    }

    static Stream<Showdown> splits(SplittableGenerator rng, int opponents, Pocket pocket, Board board) {
        final class Splits implements Spliterator<Showdown> {
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
            public boolean tryAdvance(Consumer<? super Showdown> action) {
                if (trials < 1) {
                    return false;
                } else {
                    trials--;
                    deck.shuffle();
                    var board = deck.deal(partial, deck.size() - 45);
                    var value = pocket.evaluate(board);
                    Showdown outcome = WIN;
                    for (var n = 0; n < opponents; n++) {
                        switch (signum(value - deck.deal(board, 2).evaluate())) {
                            case 0: outcome = nextSplit(outcome);
                            case 1: continue;
                        }
                        outcome = LOSS;
                        break;
                    }
                    action.accept(outcome);
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
}
