package io.github.gdejohn.monty;

import static io.github.gdejohn.monty.Monty.Showdown.LOSS;
import static io.github.gdejohn.monty.Monty.Showdown.WIN;
import static java.math.MathContext.DECIMAL128;
import static java.util.stream.Collector.Characteristics.UNORDERED;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.Spliterator;
import java.util.SplittableRandom;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.github.gdejohn.monty.Card.Cards;
import io.github.gdejohn.monty.Monty.Showdown.Tie;

public final class Monty {
    public static sealed interface Showdown {
        public static record Win() implements Showdown {
            @Override
            public int split() {
                return 1;
            }
        }

        public static record Loss() implements Showdown {
            @Override
            public int split() {
                return 0;
            }
        }

        public static record Tie(int split) implements Showdown {
            public Tie {
                if (split < 2 || split > 23) {
                    throw new IllegalArgumentException();
                }
            }

            @Override
            public double share() {
                return 1.0d / split;
            }
        }

        public static final Win WIN = new Win();

        public static final Loss LOSS = new Loss();

        default double share() {
            return split();
        }

        int split();
    }

    private Monty() {
        throw new AssertionError("this class is not intended to be instantiated");
    }

    static Stream<Showdown> simulate(SplittableRandom rng, int opponents, int trials, Pocket pocket, Board board) {
        class Showdowns implements Spliterator<Showdown> {
            private final SplittableRandom rng;

            private final Card[] deck;

            private final Pocket pocket;

            private final Hand partial;

            private long trials;

            private int bound;

            Showdowns(SplittableRandom rng, Card[] deck, Pocket pocket, Hand partial, long trials) {
                this.rng = rng;
                this.deck = deck;
                this.pocket = pocket;
                this.partial = partial;
                this.trials = trials;
            }

            Showdowns(SplittableRandom rng, Card[] deck, Pocket pocket, Board board, long trials) {
                this(rng, deck, pocket, Hand.of(board.cards), trials);
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
                        rng.split(),
                        Arrays.copyOf(deck, deck.length),
                        pocket,
                        partial,
                        trials - (trials >>>= 1)
                    );
                }
            }

            private void shuffle() {
                bound = deck.length;
            }

            private Hand deal(Hand partial, int n) {
                while (n-- > 0) {
                    var index = rng.nextInt(bound--);
                    var card = deck[index];
                    deck[index] = deck[bound];
                    deck[bound] = card;
                    partial = partial.deal(card);
                }
                return partial;
            }

            private static Hand hand(Hand partial, Pocket pocket) {
                for (var index = 0; index < 2; index++) {
                    partial = partial.deal(pocket.cards[index]);
                }
                return partial;
            }

            @Override
            public boolean tryAdvance(Consumer<? super Showdown> action) {
                if (trials-- < 1) {
                    return false;
                } else {
                    shuffle();
                    var board = deal(partial, deck.length - 45);
                    var value = hand(board, pocket).evaluate();
                    Showdown outcome = WIN;
                    for (var i = 0; i < opponents; i++) {
                        switch (Integer.signum(value - deal(board, 2).evaluate())) {
                            case 0: outcome = new Tie(outcome.split() + 1);
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
                String.format("opponents = %d (must be greater than 0 and less than 23)", opponents)
            );
        } else if ((pocket.pack() & board.pack()) != 0) {
            throw new IllegalArgumentException(
                String.format(
                    "pocket %s and board %s must be disjoint, but both contain %s",
                    pocket,
                    board,
                    Cards.toString(Cards.unpack(pocket.pack() & board.pack()))
                )
            );
        } else {
            var cards = Card.DECK ^ board.pack() ^ pocket.pack();
            var deck = new Card[Long.bitCount(cards)];
            for (var index = 0; index < deck.length; index++) {
                var card = cards & -cards;
                deck[index] = Card.unpack(card);
                cards ^= card;
            }
            return StreamSupport.stream(
                new Showdowns(rng, deck, pocket, board, trials),
                true // parallel
            );
        }
    }

    public static Stream<Showdown> simulate(int opponents, int trials, Pocket pocket, Board board) {
        return simulate(new SplittableRandom(), opponents, trials, pocket, board);
    }

    public static Stream<Showdown> simulate(int opponents, int trials, Pocket pocket) {
        return simulate(opponents, trials, pocket, Board.PRE_FLOP);
    }

    public static Collector<Showdown, ?, BigDecimal> equity() {
        return equity(DECIMAL128);
    }

    public static Collector<Showdown, ?, BigDecimal> equity(MathContext context) {
        final class Equity {
            /**
             * Least common multiple of the integers from 1 to 23, inclusive.
             */
            private static final long pot = 5_354_228_880L;

            private static final long[] shares = shares();

            private static long[] shares() {
                var shares = new long[24];
                for (var split = 1; split < shares.length; split++) {
                    shares[split] = pot / split;
                }
                return shares;
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
        return expectedValue(pot, raise, DECIMAL128);
    }

    public static Collector<Showdown, ?, BigDecimal> expectedValue(long pot, long raise, MathContext context) {
        if (pot <= 0.0d) {
            throw new IllegalArgumentException(String.format("pot = %f (must be positive)", pot));
        } else if (raise <= 0.0d) {
            throw new IllegalArgumentException(String.format("raise = %f (must be positive)", raise));
        } else {
            return expectedValue(BigDecimal.valueOf(pot), BigDecimal.valueOf(raise), context);
        }
    }

    private static Collector<Showdown, ?, BigDecimal> expectedValue(BigDecimal pot, BigDecimal raise, MathContext context) {
        return Collectors.collectingAndThen(
            equity(context),
            equity -> equity.multiply(pot.add(raise)).divide(raise, context)
        );
    }
}
