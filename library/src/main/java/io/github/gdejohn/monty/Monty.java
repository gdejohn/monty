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

import static io.github.gdejohn.monty.Deck.deck;
import static java.lang.Integer.signum;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static java.math.RoundingMode.HALF_EVEN;
import static java.util.stream.IntStream.range;
import static java.util.stream.IntStream.rangeClosed;

public final class Monty {
    public static final class Builder {
        private final SplittableGenerator rng;

        private Builder(SplittableGenerator rng) {
            this.rng = rng;
        }

        public Builder.Board players(int players) {
            return new Board(players);
        }

        public Builder.Board headsUp() {
            return players(2);
        }

        public final class Board {
            private final int players;

            private Board(int players) {
                this.players = players;
            }

            public Board.Pocket preflop() {
                return new Pocket();
            }

            public Board.Pocket flop(Card first, Card second, Card third) {
                return new Pocket(first, second, third);
            }

            public Board.Pocket turn(Card first, Card second, Card third, Card fourth) {
                return new Pocket(first, second, third, fourth);
            }

            public Board.Pocket river(Card first, Card second, Card third, Card fourth, Card fifth) {
                return new Pocket(first, second, third, fourth, fifth);
            }

            public final class Pocket {
                private final io.github.gdejohn.monty.Board board;

                private Pocket(Card... cards) {
                    this.board = new io.github.gdejohn.monty.Board(cards);
                }

                public Monty pocket(Card first, Card second) {
                    var pocket = new io.github.gdejohn.monty.Pocket(first, second);
                    return new Monty(pocket, board, players, rng);
                }
            }
        }
    }

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

    public static Monty.Builder rng(SplittableGenerator rng) {
        return new Builder(rng);
    }

    static Monty.Builder rng(byte[] seed) {
        return rng(new Generator(seed));
    }

    public static Monty.Builder.Board players(int players) {
        return rng(new Generator()).players(players);
    }

    public static Monty.Builder.Board headsUp() {
        return players(2);
    }

    public Showdown trials(long trials) {
        return stream().limit(trials).collect(
            Showdown::new,
            Showdown::accumulate,
            Showdown::combine
        );
    }

    /**
     * A lazy, infinite, parallel stream of simulated game outcomes.
     * <p>
     * The outcome of a game is represented by a nonnegative integer indicating the number of
     * players that the player with the given hole cards splits the pot with, including that
     * player: 0 means the player that player lost, 1 means that player won, and n > 1 means an
     * n-way tie.
     */
    public IntStream stream() {
        var parallel = true;
        var spliterator = new Simulation();
        return StreamSupport.intStream(spliterator, parallel);
    }

    private final class Simulation implements Spliterator.OfInt {
        private final Deck deck;

        private long trials;

        private Simulation(Deck deck, long trials) {
            this.deck = deck;
            this.trials = trials;
        }

        Simulation() {
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

    /** Least common multiple of integers ranging from 2 to n, inclusive. */
    private static BigInteger lcm(int n) {
        return rangeClosed(2, n).mapToObj(BigInteger::valueOf).reduce(
            ONE,
            (a, b) -> a.multiply(b.divide(a.gcd(b)))
        );
    }

    private static final long[][] pots = range(0, 24).mapToObj(
        players -> rangeClosed(0, players).mapToObj(BigInteger::valueOf).map(
            split -> split.equals(ZERO) ? ZERO : lcm(players).divide(split)
        ).mapToLong(BigInteger::longValueExact).toArray()
    ).toArray(long[][]::new);

    public final class Showdown {
        private static final MathContext DEFAULT_CONTEXT = new MathContext(4, HALF_EVEN);

        private final long[] pot;

        private long winnings;

        private long trials;

        private Showdown() {
            this.pot = pots[players];
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
