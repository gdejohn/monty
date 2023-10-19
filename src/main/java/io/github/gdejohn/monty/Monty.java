package io.github.gdejohn.monty;

import static java.lang.Integer.signum;
import java.math.BigDecimal;
import java.math.MathContext;
import static java.math.RoundingMode.HALF_EVEN;

import java.util.EnumSet;
import java.util.Spliterator;
import java.util.SplittableRandom;
import java.util.function.Consumer;
import java.util.stream.Collector;
import static java.util.stream.Collector.Characteristics.UNORDERED;
import static java.util.stream.Collectors.collectingAndThen;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.github.gdejohn.monty.Card.Cards;
import static io.github.gdejohn.monty.Deck.deck;
import static io.github.gdejohn.monty.Showdown.LOSS;
import io.github.gdejohn.monty.Showdown.Tie;
import static io.github.gdejohn.monty.Showdown.WIN;
import static java.util.stream.Collectors.toSet;

public final class Monty {
    private static final MathContext DEFAULT_CONTEXT = new MathContext(4, HALF_EVEN);

    private Monty() {
        throw new AssertionError("this class is not intended to be instantiated");
    }

    public static Stream<Showdown> simulate(int opponents, int trials, Pocket pocket, Board board) {
        return simulate(new SplittableRandom(), opponents, trials, pocket, board);
    }

    public static Stream<Showdown> simulate(int opponents, int trials, Pocket pocket) {
        return simulate(opponents, trials, pocket, Board.PRE_FLOP);
    }

    static Stream<Showdown> simulate(SplittableRandom rng, int opponents, int trials, Pocket pocket, Board board) {
        class Showdowns implements Spliterator<Showdown> {
            private final Deck deck;

            private final Pocket pocket;

            private final Hand partial;

            private int trials;

            Showdowns(Deck deck, Pocket pocket, Hand partial, int trials) {
                this.deck = deck;
                this.pocket = pocket;
                this.partial = partial;
                this.trials = trials;
            }

            Showdowns(SplittableRandom rng, Pocket pocket, Board board, int trials) {
                this(
                    deck(board, pocket, rng),
                    pocket,
                    board.hand(),
                    trials
                );
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
            public Showdowns trySplit() {
                if (trials < 2) {
                    return null;
                } else {
                    return new Showdowns(
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
                    var board = deck.deal(partial, deck.size() - 45);
                    var value = pocket.evaluate(board);
                    Showdown outcome = WIN;
                    for (var i = 0; i < opponents; i++) {
                        switch (signum(value - deck.deal(board, 2).evaluate())) {
                            case 0: outcome = new Tie(outcome.split() + 1);
                            case 1: continue;
                        }
                        outcome = LOSS;
                        break;
                    }
                    deck.shuffle();
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
                new Showdowns(rng, pocket, board, trials),
                true // parallel
            );
        }
    }

    public static Collector<Showdown, ?, BigDecimal> equity() {
        return equity(DEFAULT_CONTEXT);
    }

    public static Collector<Showdown, ?, BigDecimal> equity(MathContext context) {
        final class Equity {
            /**
             * Least common multiple of the integers from 1 to 23, inclusive.
             */
            private static final long pot = 5_354_228_880L;

            private static final long[] shares;

            static {
                shares = new long[24];
                for (var split = 1; split < shares.length; split++) {
                    shares[split] = pot / split;
                }
            }
            
            private long winnings = 0L;

            private int trials = 0;
            
            void accumulate(Showdown showdown) {
                winnings += shares[showdown.split()];
                trials++;
            }

            Equity combine(Equity that) {
                this.winnings += that.winnings;
                this.trials += that.trials;
                return this;
            }

            BigDecimal finish() {
                return BigDecimal.valueOf(winnings).divide(
                    BigDecimal.valueOf(pot).multiply(BigDecimal.valueOf(trials)),
                    context
                );
            }
        }

        return Collector.of(
            Equity::new,
            Equity::accumulate,
            Equity::combine,
            Equity::finish,
            UNORDERED
        );
    }

    public static Collector<Showdown, ?, BigDecimal> expectedValue(long pot, long raise) {
        return expectedValue(pot, raise, DEFAULT_CONTEXT);
    }

    public static Collector<Showdown, ?, BigDecimal> expectedValue(long pot, long raise, MathContext context) {
        if (pot <= 0.0d) {
            throw new IllegalArgumentException(STR."pot = \{pot} (must be positive)");
        } else if (raise <= 0.0d) {
            throw new IllegalArgumentException(STR."raise = \{raise} (must be positive)");
        } else {
            return expectedValue(BigDecimal.valueOf(pot), BigDecimal.valueOf(raise), context);
        }
    }

    private static Collector<Showdown, ?, BigDecimal> expectedValue(BigDecimal pot, BigDecimal raise, MathContext context) {
        return collectingAndThen(
            equity(context),
            equity -> equity.multiply(pot.add(raise)).divide(raise, context)
        );
    }
}
