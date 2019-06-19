package io.github.gdejohn.monty;

import java.util.Arrays;
import java.util.Spliterator;
import java.util.SplittableRandom;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public final class Monty {
    private static final class Dealer implements Spliterator.OfInt {
        private final SplittableRandom random;

        private final long[] deck;

        private final long pocket;

        private final long board;

        private final int opponents;

        private int trials;

        private int bound;

        private Dealer(SplittableRandom random, long[] deck, long pocket, long board, int opponents, int trials) {
            this.random = random;
            this.deck = deck;
            this.pocket = pocket;
            this.board = board;
            this.opponents = opponents;
            this.trials = trials;
        }

        private void shuffle() {
            bound = deck.length;
        }

        private long deal(int n) {
            var cards = 0L;
            while (n-- > 0) {
                var index = random.nextInt(bound--);
                var card = deck[index];
                deck[index] = deck[bound];
                deck[bound] = card;
                cards |= card;
            }
            return cards;
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            if (trials < 1) {
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
                action.accept(split);
                trials--;
                return true;
            }
        }

        @Override
        public Dealer trySplit() {
            if (this.trials <= 1) {
                return null;
            } else {
                var trials = this.trials >> 1;
                this.trials -= trials;
                return new Dealer(random.split(), Arrays.copyOf(deck, deck.length), pocket, board, opponents, trials);
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

    public static final class Board {
        private final long cards;

        private Board(long cards) {
            this.cards = cards;
        }

        private Board() {
            this(0L);
        }

        long cards() {
            return cards;
        }
    }

    public static final class Pocket {
        private final long cards;

        private Pocket(long cards) {
            this.cards = cards;
        }

        long cards() {
            return cards;
        }
    }

    public static final class Simulation {
        private final int[] splits;

        private final int trials;

        private Simulation(int[] splits, int trials) {
            this.splits = splits;
            this.trials = trials;
        }

        /**
         * Estimated average winnings, expressed as a fractional pot.
         */
        public double equity() {
            return IntStream.range(1, splits.length).mapToDouble(
                index -> (double) splits[index] / index
            ).sum() / trials;
        }

        /**
         * Estimated average winnings, expressed as a fraction of the raise.
         */
        public double expectedValue(double pot, double raise) {
            if (pot <= 0.0d) {
                throw new IllegalArgumentException(String.format("pot = %f (must be positive)", pot));
            } else if (raise <= 0.0d) {
                throw new IllegalArgumentException(String.format("raise = %f (must be positive)", raise));
            } else {
                return this.equity() * (pot + raise) / raise;
            }
        }
    }

    private static final int DEFAULT_TRIALS = 1 << 20;

    private Monty() {
        throw new AssertionError("this class is not intended to be instantiated");
    }

    public static Pocket pocket(Card first, Card second) {
        var cards = first.pack() | second.pack();
        Card.count(2, cards);
        return new Pocket(cards);
    }

    public static Board board(Card first, Card second, Card third) {
        var cards = first.pack() | second.pack() | third.pack();
        Card.count(3, cards);
        return new Board(cards);
    }

    public static Board board(Card first, Card second, Card third, Card fourth) {
        var cards = first.pack() | second.pack() | third.pack() | fourth.pack();
        Card.count(4, cards);
        return new Board(cards);
    }

    public static Board board(Card first, Card second, Card third, Card fourth, Card fifth) {
        var cards = first.pack() | second.pack() | third.pack() | fourth.pack() | fifth.pack();
        Card.count(5, cards);
        return new Board(cards);
    }

    private static Dealer dealer(SplittableRandom random, long pocket, long board, int opponents, int trials) {
        if (opponents < 1) {
            throw new IllegalArgumentException(String.format("opponents = %d (must be positive)", opponents));
        } else if (opponents > 22) {
            throw new IllegalArgumentException(String.format("opponents = %d (must be less than 23)", opponents));
        } else if (trials < 1) {
            throw new IllegalArgumentException(String.format("trials = %d (must be positive)", trials));
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
            return new Dealer(random, deck, pocket, board, opponents, trials);
        }
    }

    static Simulation simulation(int opponents, Pocket pocket, Board board, int trials, SplittableRandom random) {
        var dealer = dealer(random, pocket.cards(), board.cards(), opponents, trials);
        var splits = StreamSupport.intStream(dealer, true).collect(
            () -> new int[opponents + 2],
            (pots, split) -> pots[split]++,
            (first, second) -> IntStream.range(0, first.length).forEach(
                split -> first[split] += second[split]
            )
        );
        return new Simulation(splits, trials);
    }

    public static Simulation simulation(int opponents, Pocket pocket, Board board) {
        return simulation(opponents, pocket, board, DEFAULT_TRIALS, new SplittableRandom());
    }

    public static Simulation simulation(int opponents, Pocket pocket) {
        return simulation(opponents, pocket, new Board());
    }
}
