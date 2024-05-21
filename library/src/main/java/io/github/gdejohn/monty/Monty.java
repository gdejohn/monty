package io.github.gdejohn.monty;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Spliterator;
import java.util.SplittableRandom;
import java.util.function.IntConsumer;
import java.util.random.RandomGenerator.SplittableGenerator;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

import static io.github.gdejohn.monty.Board.preflop;
import static io.github.gdejohn.monty.Deck.deck;
import static java.lang.Integer.signum;
import static java.math.MathContext.UNLIMITED;
import static java.math.RoundingMode.HALF_EVEN;

public final class Monty {
    private static final MathContext DEFAULT_CONTEXT = new MathContext(4, HALF_EVEN);

    /** Least common multiple of the integers from 1 to 23, inclusive. */
    private static final BigDecimal POT = BigDecimal.valueOf(5_354_228_880L);

    private static final long[] WINNINGS = LongStream.range(0, 24).map(
        split -> split == 0 ? 0 : POT.divide(BigDecimal.valueOf(split), UNLIMITED).longValueExact()
    ).toArray();

    private long winnings = 0;

    private long trials = 0;

    public Monty() {}

    public void accumulate(int split) {
        winnings = Math.addExact(winnings, WINNINGS[split]);
        trials++;
    }

    public void combine(Monty monty) {
        winnings = Math.addExact(winnings, monty.winnings);
        trials += monty.trials;
    }

    private BigDecimal winnings() {
        return BigDecimal.valueOf(winnings);
    }

    private BigDecimal trials() {
        return BigDecimal.valueOf(trials).multiply(POT);
    }

    public BigDecimal equity() {
        return equity(DEFAULT_CONTEXT);
    }

    public BigDecimal equity(MathContext context) {
        return winnings().divide(trials(), context);
    }

    public BigDecimal expectedValue(long raise, long pot) {
        return expectedValue(raise, pot, DEFAULT_CONTEXT);
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

    private BigDecimal expectedValue(BigDecimal raise, BigDecimal pot, MathContext context) {
        return winnings().multiply(pot.add(raise)).divide(trials().multiply(raise), context);
    }

    /** Heads up, preflop. */
    public static IntStream showdown(Pocket pocket) {
        return showdown(pocket, 1);
    }

    public static IntStream showdown(Pocket pocket, Board board) {
        return showdown(pocket, board, 1);
    }

    public static IntStream showdown(Pocket pocket, int opponents) {
        return showdown(pocket, preflop(), opponents);
    }

    /**
     * A lazy, infinite stream of simulated game outcomes.
     * <p>
     * The outcome of a game is represented by an unsigned integer indicating the number of
     * players that the player with the given hole cards splits the pot with, including that
     * player: 0 means the player with the given hole cards lost, 1 means that player won, and
     * n > 1 means an n-way tie.
     */
    public static IntStream showdown(Pocket pocket, Board board, int opponents) {
        return showdown(pocket, board, opponents, new SplittableRandom());
    }

    public static IntStream showdown(Pocket pocket, SplittableGenerator rng) {
        return showdown(pocket, 1, rng);
    }

    public static IntStream showdown(Pocket pocket, Board board, SplittableGenerator rng) {
        return showdown(pocket, board, 1, rng);
    }

    public static IntStream showdown(Pocket pocket, int opponents, SplittableGenerator rng) {
        return showdown(pocket, preflop(), opponents, rng);
    }

    public static IntStream showdown(Pocket pocket, Board board, int opponents, SplittableGenerator rng) {
        if (opponents < 1 || opponents > 22) {
            throw new IllegalArgumentException(
                "opponents = %d (must be greater than 0 and less than 23)".formatted(opponents)
            );
        } else if ((pocket.cards() & board.cards()) != 0) {
            throw new IllegalArgumentException(
                "pocket %s and board %s must be disjoint, but both contain %s".formatted(
                    pocket,
                    board,
                    Card.toString(Card.stream(pocket.cards() & board.cards()))
                )
            );
        }
        var parallel = true;
        var spliterator = new Showdown(pocket, board, opponents, rng);
        return StreamSupport.intStream(spliterator, parallel);
    }

    private static final class Showdown implements Spliterator.OfInt {
        private final Pocket pocket;

        private final Board board;

        private final int opponents;

        private final Deck deck;

        private long trials;

        private Showdown(Pocket pocket, Board board, int opponents, Deck deck, long trials) {
            this.pocket = pocket;
            this.board = board;
            this.opponents = opponents;
            this.deck = deck;
            this.trials = trials;
        }

        Showdown(Pocket pocket, Board board, int opponents, SplittableGenerator rng) {
            this(pocket, board, opponents, deck(board, pocket, rng), Long.MAX_VALUE);
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
        public Spliterator.OfInt trySplit() {
            if (trials < 2) {
                return null;
            } else {
                return new Showdown(
                    pocket,
                    board,
                    opponents,
                    deck.split(),
                    trials - (trials >>>= 1)
                );
            }
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            if (trials < 1) {
                return false;
            }
            trials--;
            deck.shuffle();
            var hand = deck.deal(board);
            int value = pocket.evaluate(hand);
            int split = 1;
            for (int n = 0; n < opponents; n++) {
                switch (signum(value - deck.deal(hand).evaluate())) {
                    case +0: split++;
                    case +1: continue;
                    case -1: split = 0;
                }
                break;
            }
            action.accept(split);
            return true;
        }
    }
}
