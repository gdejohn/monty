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
import static io.github.gdejohn.monty.Showdown.LCM;
import static io.github.gdejohn.monty.Showdown.showdown;
import static java.lang.Integer.signum;
import static java.math.MathContext.UNLIMITED;
import static java.math.RoundingMode.HALF_EVEN;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.summarizingLong;

public final class Monty {
    private static final MathContext DEFAULT_CONTEXT = new MathContext(4, HALF_EVEN);

    final BigDecimal winnings;
    final BigDecimal trials;

    private Monty(LongSummaryStatistics stats) {
        this.winnings = BigDecimal.valueOf(stats.getSum());
        this.trials = BigDecimal.valueOf(stats.getCount()).multiply(LCM);
    }

    public static Collector<Showdown, ?, Monty> monty() {
        return collectingAndThen(summarizingLong(Showdown::winnings), Monty::new);
    }

    public long trials() {
        return trials.divide(LCM, UNLIMITED).longValueExact();
    }

    public BigDecimal equity() {
        return equity(DEFAULT_CONTEXT);
    }

    public BigDecimal equity(MathContext context) {
        return winnings.divide(trials, context);
    }

    private BigDecimal expectedValue(BigDecimal raise, BigDecimal pot, MathContext context) {
        return winnings.multiply(pot.add(raise)).divide(
            trials.multiply(raise),
            context
        );
    }

    public BigDecimal expectedValue(long raise, long pot, MathContext context) {
        if (raise <= 0L) {
            throw new IllegalArgumentException("raise = %d (must be positive)".formatted(raise));
        } else if (pot <= 0L) {
            throw new IllegalArgumentException("pot = %d (must be positive)".formatted(pot));
        } else {
            return expectedValue(BigDecimal.valueOf(raise), BigDecimal.valueOf(pot), context);
        }
    }

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
            public Spliterator<Showdown> trySplit() {
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
                    var split = 1;
                    for (var n = 0; n < opponents; n++) {
                        switch (signum(value - deck.deal(board, 2).evaluate())) {
                            case 0: split++;
                            case 1: continue;
                        }
                        split = 0;
                        break;
                    }
                    action.accept(showdown(split));
                    return true;
                }
            }
        }

        if (opponents < 1 || opponents > 22) {
            throw new IllegalArgumentException(
                "opponents = %d (must be greater than 0 and less than 23)".formatted(opponents)
            );
        } else if ((pocket.pack() & board.pack()) != 0) {
            throw new IllegalArgumentException(
                "pocket %s and board %s must be disjoint, but both contain %s".formatted(
                    pocket,
                    board,
                    Cards.toString(Cards.unpack(pocket.pack() & board.pack()))
                )
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
