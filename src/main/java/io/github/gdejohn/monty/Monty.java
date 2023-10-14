package io.github.gdejohn.monty;

import static io.github.gdejohn.monty.Card.unpack;
import static io.github.gdejohn.monty.Monty.Equity.pot;

import java.util.Arrays;
import java.util.Spliterator;
import java.util.SplittableRandom;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.github.gdejohn.monty.Card.Board;
import io.github.gdejohn.monty.Card.Pocket;

public final class Monty {
    public static final class Equity {
        private static final Equity[] pot = pot();

        private static Equity[] pot() {
            var pot = new Equity[24];
            pot[0] = new Equity(0.0d);
            IntStream.range(1, pot.length).forEach(split -> pot[split] = new Equity(1.0d / split));
            return pot;
        }

        private final double share;

        private Equity(double share) {
            this.share = share;
        }

        static Equity pot(int split) {
            return pot[split];
        }

        public double share() {
            return share;
        }
    }

    private Monty() {
        throw new AssertionError("this class is not intended to be instantiated");
    }

    static Stream<Equity> simulate(SplittableRandom rng, int opponents, Pocket pocket, Board board) {
        class Simulator implements Spliterator<Equity> {
            private final SplittableRandom rng;

            private final long[] deck;

            private final long[] pocket;

            private final Hand board;

            private long trials;

            private int bound;

            Simulator(SplittableRandom rng, long[] deck, long[] pocket, Hand board, long trials) {
                this.rng = rng;
                this.deck = deck;
                this.pocket = pocket;
                this.board = board;
                this.trials = trials;
            }

            Simulator(SplittableRandom rng, long[] deck, long[] pocket, long[] board, long trials) {
                this(rng, deck, pocket, new Hand(board), trials);
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
            public Simulator trySplit() {
                if (trials == 1) {
                    return null;
                } else {
                    return new Simulator(
                        rng.split(),
                        Arrays.copyOf(deck, deck.length),
                        pocket,
                        board,
                        trials - (trials >>>= 1)
                    );
                }
            }

            private void shuffle() {
                bound = deck.length;
            }

            private Hand deal(Hand board, int n) {
                var cards = board.copy();
                while (n-- > 0) {
                    var index = rng.nextInt(bound--);
                    var card = deck[index];
                    deck[index] = deck[bound];
                    deck[bound] = card;
                    cards.deal(card);
                }
                return cards;
            }

            private static Hand hand(Hand board, long[] pocket) {
                var hand = board.copy();
                for (var card : pocket) {
                    hand.deal(card);
                }
                return hand;
            }

            @Override
            public boolean tryAdvance(Consumer<? super Equity> action) {
                if (trials == 0) {
                    return false;
                } else {
                    shuffle();
                    var board = deal(this.board, deck.length - 45);
                    var value = hand(board, pocket).evaluate();
                    var split = 1;
                    for (var i = 0; i < opponents; i++) {
                        switch (Integer.signum(value - deal(board, 2).evaluate())) {
                            case 0: split++;
                            case 1: continue;
                        }
                        split = 0;
                        break;
                    }
                    action.accept(pot(split));
                    trials--;
                    return true;
                }
            }
        }

        if (opponents < 1 || opponents > 22) {
            throw new IllegalArgumentException(
                String.format("opponents = %d (must be greater than 0 and less than 23)", opponents)
            );
        } else if ((pocket.mask() & board.mask()) != 0) {
            throw new IllegalArgumentException(
                String.format(
                    "pocket %s and board %s must be disjoint, but both contain %s",
                    Card.toString(unpack(pocket.mask())),
                    Card.toString(unpack(board.mask())),
                    Card.toString(unpack(pocket.mask() & board.mask()))
                )
            );
        } else {
            var cards = Card.MASK ^ board.mask() ^ pocket.mask();
            var deck = new long[Long.bitCount(cards)];
            for (var index = 0; index < deck.length; index++) {
                var card = cards & -cards;
                deck[index] = pack(Long.numberOfTrailingZeros(card));
                cards ^= card;
            }
            var winnings = new Simulator(rng, deck, pocket.cards, board.cards, Long.MAX_VALUE);
            return StreamSupport.stream(winnings, true);
        }
    }

    static long pack(long card) {
        return (1L << card) | ((card % 13) << 52) | ((card / 13) << 56);
    }

    public static Stream<Equity> simulate(int opponents, Pocket pocket, Board board) {
        return simulate(new SplittableRandom(), opponents, pocket, board);
    }

    public static Stream<Equity> simulate(int opponents, Pocket pocket) {
        return simulate(opponents, pocket, Board.PRE_FLOP);
    }

    private static final Collector<Equity, ?, Double> EQUITY = Collectors.averagingDouble(Equity::share);

    public static Collector<Equity, ?, Double> equity() {
        return EQUITY;
    }

    public static Collector<Equity, ?, Double> expectedValue(double pot, double raise) {
        if (pot <= 0.0d) {
            throw new IllegalArgumentException(String.format("pot = %f (must be positive)", pot));
        } else if (raise <= 0.0d) {
            throw new IllegalArgumentException(String.format("raise = %f (must be positive)", raise));
        } else {
            return Collectors.collectingAndThen(equity(), equity -> equity * (pot + raise) / raise);
        }
    }
}
