package io.github.gdejohn.monty;

import io.github.gdejohn.monty.Card.Board;
import io.github.gdejohn.monty.Card.Pocket;

import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Monty {
    public static final class Winnings {
        private final double share;

        Winnings(double share) {
            this.share = share;
        }

        public double share() {
            return share;
        }
    }

    private static final Winnings[] winnings = winnings();

    private static Winnings[] winnings() {
        var winnings = new Winnings[24];
        winnings[0] = new Winnings(0.0d);
        for (var split = 1; split < winnings.length; split++) {
            winnings[split] = new Winnings(1.0d / split);
        }
        return winnings;
    }

    private Monty() {
        throw new AssertionError("this class is not intended to be instantiated");
    }

    static Stream<Winnings> simulate(SplittableRandom generator, int opponents, long pocket, long board, long estimatedSize) {
        class Spliterator implements java.util.Spliterator<Winnings> {
            private final SplittableRandom generator;

            private final long[] deck;

            private final long pocket;

            private final long board;

            private long estimatedSize;

            private int bound;

            Spliterator(SplittableRandom generator, long[] deck, long pocket, long board, long estimatedSize) {
                this.generator = generator;
                this.deck = deck;
                this.pocket = pocket;
                this.board = board;
                this.estimatedSize = estimatedSize;
            }

            private void shuffle() {
                bound = deck.length;
            }

            private long deal(int n) {
                var cards = 0L;
                while (n-- > 0) {
                    var index = generator.nextInt(bound--);
                    var card = deck[index];
                    deck[index] = deck[bound];
                    deck[bound] = card;
                    cards |= card;
                }
                return cards;
            }

            @Override
            public boolean tryAdvance(Consumer<? super Winnings> action) {
                this.shuffle();
                var board = this.board | deal(deck.length - 45);
                var value = Hand.evaluate(board | pocket);
                var split = 1;
                for (var opponent = 0; opponent < opponents; opponent++) {
                    switch (Integer.signum(value - Hand.evaluate(board | deal(2)))) {
                        case 0: split++;
                        case 1: continue;
                    }
                    split = 0;
                    break;
                }
                action.accept(winnings[split]);
                return true;
            }

            @Override
            public Spliterator trySplit() {
                if (estimatedSize == 0) {
                    return null;
                } else {
                    return new Spliterator(
                        generator.split(),
                        Arrays.copyOf(deck, deck.length),
                        pocket,
                        board,
                        estimatedSize >>>= 1
                    );
                }
            }

            @Override
            public long estimateSize() {
                return estimatedSize;
            }

            @Override
            public int characteristics() {
                return IMMUTABLE | NONNULL;
            }
        }

        if (opponents < 1) {
            throw new IllegalArgumentException(String.format("opponents = %d (must be positive)", opponents));
        } else if (opponents > 22) {
            throw new IllegalArgumentException(String.format("opponents = %d (must be less than 23)", opponents));
        } else if ((pocket & board) != 0) {
            throw new IllegalArgumentException("pocket and board must be disjoint");
        } else {
            var cards = (-1L >>> -52) ^ pocket ^ board;
            var deck = new long[Long.bitCount(cards)];
            for (var index = 0; index < deck.length; index++) {
                var card = cards & -cards;
                deck[index] = card;
                cards ^= card;
            }
            return StreamSupport.stream(new Spliterator(generator, deck, pocket, board, estimatedSize), true);
        }
    }

    static Stream<Winnings> simulate(SplittableRandom generator, int opponents, Pocket pocket, Board board) {
        return simulate(generator, opponents, pocket.cards(), board.cards(), Long.MAX_VALUE);
    }

    public static Stream<Winnings> simulate(int opponents, Pocket pocket, Board board) {
        return simulate(new SplittableRandom(), opponents, pocket, board);
    }

    public static Stream<Winnings> simulate(int opponents, Pocket pocket) {
        return simulate(opponents, pocket, new Board());
    }

    public static Collector<Winnings, ?, Double> equity() {
        return Collectors.averagingDouble(Winnings::share);
    }

    public static Collector<Winnings, ?, Double> expectedValue(double pot, double raise) {
        if (pot <= 0.0d) {
            throw new IllegalArgumentException(String.format("pot = %f (must be positive)", pot));
        } else if (raise <= 0.0d) {
            throw new IllegalArgumentException(String.format("raise = %f (must be positive)", raise));
        } else {
            return Collectors.collectingAndThen(equity(), equity -> equity * (pot + raise) / raise);
        }
    }
}
