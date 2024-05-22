package io.github.gdejohn.monty;

import io.github.gdejohn.monty.Deck.Generator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Spliterator;
import java.util.function.IntConsumer;
import java.util.random.RandomGenerator.SplittableGenerator;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static io.github.gdejohn.monty.Board.preflop;
import static io.github.gdejohn.monty.Deck.deck;
import static java.lang.Integer.signum;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static java.math.RoundingMode.HALF_EVEN;
import static java.util.stream.IntStream.range;
import static java.util.stream.IntStream.rangeClosed;

public final class Monty {
    private final Pocket pocket;

    private final Board board;

    private final int players;

    private final SplittableGenerator rng;

    public Monty(Pocket pocket, Board board, int players, SplittableGenerator rng) {
        if (players < 2 || players > 23) {
            throw new IllegalArgumentException(
                "players = %d (must be greater than 1 and less than 24)".formatted(players)
            );
        } else if (board.stream().anyMatch(card -> pocket.stream().anyMatch(card::equals))) {
            throw new IllegalArgumentException(
                "pocket %s and board %s must be disjoint".formatted(pocket, board)
            );
        }
        this.pocket = pocket;
        this.board = board;
        this.players = players;
        this.rng = rng;
    }

    public Monty(Pocket pocket, Board board, int players) {
        this(pocket, board, players, new Generator());
    }

    public Monty(Pocket pocket, Board board, SplittableGenerator rng) {
        this(pocket, board, 2, rng);
    }

    public Monty(Pocket pocket, Board board) {
        this(pocket, board, 2);
    }

    public Monty(Pocket pocket, int players) {
        this(pocket, preflop(), players);
    }

    public Monty(Pocket pocket) {
        this(pocket, 2);
    }

    public Showdown showdown(long trials) {
        return stream().limit(trials).collect(
            Showdown::new,
            Showdown::accumulate,
            Showdown::combine
        );
    }

    /**
     * A lazy, infinite stream of simulated game outcomes.
     * <p>
     * The outcome of a game is represented by an unsigned integer indicating the number of
     * players that the player with the given hole cards splits the pot with, including that
     * player: 0 means the player with the given hole cards lost, 1 means that player won, and
     * n > 1 means an n-way tie.
     */
    public IntStream stream() {
        var parallel = true;
        var spliterator = new Simulation(rng);
        return StreamSupport.intStream(spliterator, parallel);
    }

    private final class Simulation implements Spliterator.OfInt {
        private final Deck deck;

        private long trials;

        private Simulation(Deck deck, long trials) {
            this.deck = deck;
            this.trials = trials;
        }

        Simulation(SplittableGenerator rng) {
            this(deck(board, pocket, rng), Long.MAX_VALUE);
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
            }
            return new Simulation(deck.split(), trials - (trials >>>= 1));
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
            for (int n = 1; n < players; n++) {
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

    private static final long[][] pot = range(0, 24).mapToObj(
        players -> rangeClosed(0, players).mapToObj(BigInteger::valueOf).map(
            split -> split.equals(ZERO) ? ZERO : rangeClosed(2, players).mapToObj(BigInteger::valueOf).reduce(
                ONE,
                (a, b) -> a.multiply(b.divide(a.gcd(b)))
            ).divide(split)
        ).mapToLong(BigInteger::longValueExact).toArray()
    ).toArray(long[][]::new);

    public final class Showdown {
        private static final MathContext DEFAULT_CONTEXT = new MathContext(4, HALF_EVEN);

        private final long[] pot;

        private long winnings;

        private long trials;

        private Showdown() {
            this.pot = Monty.pot[players];
            this.winnings = 0;
            this.trials = 0;
        }

        private void accumulate(int split) {
            winnings = Math.addExact(winnings, pot[split]);
            trials++;
        }

        private void combine(Showdown showdown) {
            winnings = Math.addExact(winnings, showdown.winnings);
            trials += showdown.trials;
        }

        public BigDecimal equity() {
            return equity(DEFAULT_CONTEXT);
        }

        public BigDecimal equity(MathContext context) {
            var winnings = BigDecimal.valueOf(this.winnings);
            var trials = BigDecimal.valueOf(this.trials).multiply(BigDecimal.valueOf(pot[1]));
            return winnings.divide(trials, context);
        }

        public BigDecimal expectedValue(long raise, long pot) {
            return expectedValue(raise, pot, DEFAULT_CONTEXT);
        }

        public BigDecimal expectedValue(long raise, long pot, MathContext context) {
            if (raise <= 0L) {
                throw new IllegalArgumentException("raise = %d (must be positive)".formatted(raise));
            } else if (pot <= 0L) {
                throw new IllegalArgumentException("pot = %d (must be positive)".formatted(pot));
            }
            return expectedValue(BigDecimal.valueOf(raise), BigDecimal.valueOf(pot), context);
        }

        private BigDecimal expectedValue(BigDecimal raise, BigDecimal pot, MathContext context) {
            var winnings = BigDecimal.valueOf(this.winnings);
            var trials = BigDecimal.valueOf(this.trials).multiply(BigDecimal.valueOf(this.pot[1]));
            return winnings.multiply(pot.add(raise)).divide(trials.multiply(raise), context);
        }
    }
}
