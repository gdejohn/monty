package io.github.gdejohn.monty;

import io.github.gdejohn.monty.Card.Board;
import io.github.gdejohn.monty.Card.Pocket;

import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static io.github.gdejohn.monty.Card.unpack;
import static io.github.gdejohn.monty.Monty.Winnings.pot;

public final class Monty {
    public static final class Winnings {
        private static final Winnings[] pot = pot();

        private static Winnings[] pot() {
            var pot = new Winnings[24];
            pot[0] = new Winnings(0.0d);
            IntStream.range(1, pot.length).forEach(split -> pot[split] = new Winnings(1.0d / split));
            return pot;
        }

        private final double share;

        private Winnings(double share) {
            this.share = share;
        }

        static Winnings pot(int split) {
            return pot[split];
        }

        public double share() {
            return share;
        }
    }

    private Monty() {
        throw new AssertionError("this class is not intended to be instantiated");
    }

    static Stream<Winnings> simulate(SplittableRandom generator, int opponents, Pocket pocket, Board board) {
        class Spliterator implements java.util.Spliterator<Winnings> {
            private final SplittableRandom generator;

            private final long[] deck;

            private final long pocket;

            private final long board;

            private long trials;

            private int bound;

            Spliterator(SplittableRandom generator, long[] deck, long pocket, long board, long trials) {
                this.generator = generator;
                this.deck = deck;
                this.pocket = pocket;
                this.board = board;
                this.trials = trials;
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
                if (trials == 0) {
                    return false;
                } else {
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
                    action.accept(pot(split));
                    trials--;
                    return true;
                }
            }

            @Override
            public Spliterator trySplit() {
                if (trials == 1) {
                    return null;
                } else {
                    return new Spliterator(
                        generator.split(),
                        Arrays.copyOf(deck, deck.length),
                        pocket,
                        board,
                        trials >>>= 1
                    );
                }
            }

            @Override
            public long estimateSize() {
                return trials;
            }

            @Override
            public int characteristics() {
                return IMMUTABLE | NONNULL | SIZED | SUBSIZED;
            }
        }

        if (opponents < 1 || opponents > 22) {
            throw new IllegalArgumentException(
                String.format("opponents = %d (must be greater than 0 and less than 23)", opponents)
            );
        } else if ((pocket.cards() & board.cards()) != 0) {
            throw new IllegalArgumentException(
                String.format(
                    "pocket %s and board %s must be disjoint, but both contain %s",
                    Card.toString(unpack(pocket.cards())),
                    Card.toString(unpack(board.cards())),
                    Card.toString(unpack(pocket.cards() & board.cards()))
                )
            );
        } else {
            var cards = (-1L >>> -52) ^ pocket.cards() ^ board.cards();
            var deck = new long[Long.bitCount(cards)];
            for (var index = 0; index < deck.length; index++) {
                var card = cards & -cards;
                deck[index] = card;
                cards ^= card;
            }
            var winnings = new Spliterator(generator, deck, pocket.cards(), board.cards(), Long.MAX_VALUE);
            return StreamSupport.stream(winnings, true);
        }
    }

    public static Stream<Winnings> simulate(int opponents, Pocket pocket, Board board) {
        return simulate(new SplittableRandom(), opponents, pocket, board);
    }

    public static Stream<Winnings> simulate(int opponents, Pocket pocket) {
        return simulate(opponents, pocket, Board.PRE_FLOP);
    }

    private static final Collector<Winnings, ?, Double> EQUITY = Collectors.averagingDouble(Winnings::share);

    public static Collector<Winnings, ?, Double> equity() {
        return EQUITY;
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
